package com.shop.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shop.common.OrderStatus;
import com.shop.dto.BatchOperationResult;
import com.shop.dto.ImportErrorDTO;
import com.shop.dto.ImportResult;
import com.shop.dto.ReviewReplyRequest;
import com.shop.dto.ReviewVO;
import com.shop.dto.ShipRequest;
import com.shop.dto.UserVO;
import com.shop.entity.Category;
import com.shop.entity.OrderMain;
import com.shop.entity.OrderOperationLog;
import com.shop.entity.Product;
import com.shop.entity.User;
import com.shop.mapper.CategoryMapper;
import com.shop.mapper.OrderMainMapper;
import com.shop.mapper.OrderOperationLogMapper;
import com.shop.mapper.ProductMapper;
import com.shop.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AdminService {

    private static final Logger log = LoggerFactory.getLogger(AdminService.class);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final BigDecimal MIN_PRICE = new BigDecimal("0.01");
    private static final String[] REQUIRED_COLUMNS = {"name", "categoryId", "price", "stock"};
    private static final String[] CSV_HEADERS = {"ID", "商品名称", "副标题", "分类ID", "分类名称", "价格", "库存", "销量", "状态", "创建时间"};

    private final UserMapper userMapper;
    private final ReviewService reviewService;
    private final OrderMainMapper orderMainMapper;
    private final OrderOperationLogMapper orderOperationLogMapper;
    private final ProductMapper productMapper;
    private final CategoryMapper categoryMapper;
    private final PriceAlertService priceAlertService;

    public List<UserVO> listUsers() {
        List<User> list = userMapper.selectAll();
        List<UserVO> result = new ArrayList<>();
        for (User u : list) {
            UserVO vo = new UserVO();
            vo.setId(u.getId());
            vo.setUsername(u.getUsername());
            vo.setNickname(u.getNickname());
            vo.setPhone(u.getPhone());
            vo.setEmail(u.getEmail());
            vo.setAvatar(u.getAvatar());
            vo.setStatus(u.getStatus());
            vo.setRole(u.getRole());
            vo.setCreatedAt(u.getCreatedAt());
            result.add(vo);
        }
        return result;
    }

    @Transactional(rollbackFor = Exception.class)
    public void updateUser(Long id, Integer status, String role) {
        User user = userMapper.selectById(id);
        if (user == null) {
            throw new IllegalArgumentException("用户不存在");
        }
        if (status != null) {
            user.setStatus(status);
        }
        if (role != null && !role.isEmpty()) {
            user.setRole(role);
        }
        userMapper.updateById(user);
        log.info("Admin updated user: id={}, status={}, role={}", id, status, role);
    }

    public List<ReviewVO> listAllReviews() {
        return reviewService.listAll();
    }

    public void deleteReview(Long id) {
        reviewService.deleteById(id);
    }

    @Transactional(rollbackFor = Exception.class)
    public void replyReview(Long adminId, String adminName, ReviewReplyRequest req) {
        reviewService.reply(adminId, adminName, req);
    }

    public List<OrderMain> listAllOrders() {
        return orderMainMapper.selectAll();
    }

    public List<OrderMain> listOrdersByStatus(Integer status) {
        if (status == null) {
            return orderMainMapper.selectAll();
        }
        return orderMainMapper.selectByStatus(status);
    }

    public OrderMain getOrderById(Long id) {
        return orderMainMapper.selectById(id);
    }

    @Transactional(rollbackFor = Exception.class)
    public void ship(ShipRequest req, Long adminId, String adminName) {
        if (req == null || req.getOrderId() == null) {
            throw new IllegalArgumentException("订单ID不能为空");
        }
        if (req.getLogisticsCompany() == null || req.getLogisticsCompany().trim().isEmpty()) {
            throw new IllegalArgumentException("物流公司不能为空");
        }
        if (req.getTrackingNo() == null || req.getTrackingNo().trim().isEmpty()) {
            throw new IllegalArgumentException("运单号不能为空");
        }
        OrderMain order = orderMainMapper.selectById(req.getOrderId());
        if (order == null) {
            throw new IllegalArgumentException("订单不存在");
        }
        int from = order.getStatus();
        int to = OrderStatus.SHIPPED;
        if (!OrderStatus.canTransition(from, to)) {
            throw new IllegalArgumentException("订单状态不允许发货：当前 " + OrderStatus.text(from)
                    + "，仅「已付款」可发货");
        }
        orderMainMapper.updateShipInfo(
                req.getOrderId(),
                to,
                req.getLogisticsCompany().trim(),
                req.getTrackingNo().trim(),
                req.getShippingRemark() != null ? req.getShippingRemark().trim() : null,
                LocalDateTime.now()
        );
        try {
            OrderOperationLog opl = new OrderOperationLog();
            opl.setOrderId(order.getId());
            opl.setOrderNo(order.getOrderNo());
            opl.setOperatorId(adminId);
            opl.setOperatorName(adminName != null ? adminName : ("admin-" + adminId));
            opl.setOperatorRole("admin");
            opl.setOperation(OrderStatus.OP_SHIP);
            opl.setOldStatus(from);
            opl.setNewStatus(to);
            opl.setRemark(req.getShippingRemark());
            Map<String, Object> extra = new HashMap<>();
            extra.put("logisticsCompany", req.getLogisticsCompany().trim());
            extra.put("trackingNo", req.getTrackingNo().trim());
            try {
                opl.setExtraInfo(OBJECT_MAPPER.writeValueAsString(extra));
            } catch (JsonProcessingException ignored) {}
            orderOperationLogMapper.insert(opl);
        } catch (Exception e) {
            log.warn("写入订单操作日志失败", e);
        }
        log.info("Admin shipped order: orderId={}, company={}, trackingNo={}",
                req.getOrderId(), req.getLogisticsCompany(), req.getTrackingNo());
    }

    @Transactional(rollbackFor = Exception.class)
    public BatchOperationResult batchUpdateStatus(List<Long> productIds, Integer status) {
        int successCount = 0;
        int failCount = 0;
        List<String> failReasons = new ArrayList<>();

        for (Long id : productIds) {
            try {
                Product product = productMapper.selectById(id);
                if (product == null) {
                    failCount++;
                    failReasons.add("商品ID " + id + " 不存在");
                    continue;
                }
                int rows = productMapper.batchUpdateStatus(List.of(id), status);
                if (rows > 0) {
                    successCount++;
                } else {
                    failCount++;
                    failReasons.add("商品ID " + id + " 更新失败");
                }
            } catch (Exception e) {
                failCount++;
                failReasons.add("商品ID " + id + " 异常: " + e.getMessage());
                log.error("批量更新状态失败，商品ID: {}", id, e);
            }
        }

        return new BatchOperationResult(successCount, failCount, failReasons);
    }

    @Transactional(rollbackFor = Exception.class)
    public BatchOperationResult batchUpdateCategory(List<Long> productIds, Long categoryId) {
        int successCount = 0;
        int failCount = 0;
        List<String> failReasons = new ArrayList<>();

        Category category = categoryMapper.selectById(categoryId);
        if (category == null) {
            failReasons.add("分类ID " + categoryId + " 不存在");
            return new BatchOperationResult(0, productIds.size(), failReasons);
        }

        for (Long id : productIds) {
            try {
                Product product = productMapper.selectById(id);
                if (product == null) {
                    failCount++;
                    failReasons.add("商品ID " + id + " 不存在");
                    continue;
                }
                int rows = productMapper.batchUpdateCategory(List.of(id), categoryId);
                if (rows > 0) {
                    successCount++;
                } else {
                    failCount++;
                    failReasons.add("商品ID " + id + " 更新失败");
                }
            } catch (Exception e) {
                failCount++;
                failReasons.add("商品ID " + id + " 异常: " + e.getMessage());
                log.error("批量更新分类失败，商品ID: {}", id, e);
            }
        }

        return new BatchOperationResult(successCount, failCount, failReasons);
    }

    @Transactional(rollbackFor = Exception.class)
    public BatchOperationResult batchUpdatePrice(List<Long> productIds, BigDecimal priceRatio) {
        if (priceRatio == null || priceRatio.compareTo(BigDecimal.ZERO) <= 0 || priceRatio.compareTo(new BigDecimal("10")) > 0) {
            throw new IllegalArgumentException("调价比例必须在0-10之间（不含0）");
        }

        int successCount = 0;
        int failCount = 0;
        List<String> failReasons = new ArrayList<>();

        for (Long id : productIds) {
            try {
                Product product = productMapper.selectById(id);
                if (product == null) {
                    failCount++;
                    failReasons.add("商品ID " + id + " 不存在");
                    continue;
                }
                if (product.getPrice() == null) {
                    failCount++;
                    failReasons.add("商品ID " + id + " 价格为空");
                    continue;
                }

                BigDecimal newPrice = product.getPrice().multiply(priceRatio)
                        .setScale(2, RoundingMode.HALF_UP);
                if (newPrice.compareTo(MIN_PRICE) < 0) {
                    newPrice = MIN_PRICE;
                }

                int rows = productMapper.batchUpdatePrice(id, newPrice);
                if (rows > 0) {
                    successCount++;
                    try {
                        priceAlertService.checkAndNotify(id);
                    } catch (Exception e) {
                        log.warn("价格变动通知失败，商品ID: {}", id, e);
                    }
                } else {
                    failCount++;
                    failReasons.add("商品ID " + id + " 更新失败");
                }
            } catch (Exception e) {
                failCount++;
                failReasons.add("商品ID " + id + " 异常: " + e.getMessage());
                log.error("批量调价失败，商品ID: {}", id, e);
            }
        }

        return new BatchOperationResult(successCount, failCount, failReasons);
    }

    @Transactional(rollbackFor = Exception.class)
    public ImportResult importProducts(InputStream inputStream) {
        int successCount = 0;
        int failCount = 0;
        List<ImportErrorDTO> errors = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            String headerLine = reader.readLine();
            if (headerLine == null) {
                errors.add(new ImportErrorDTO(1, "header", "CSV文件为空"));
                return new ImportResult(0, 1, errors);
            }

            String[] headers = headerLine.split(",");
            Map<String, Integer> headerIndex = new HashMap<>();
            for (int i = 0; i < headers.length; i++) {
                headerIndex.put(headers[i].trim().toLowerCase(), i);
            }

            for (String col : REQUIRED_COLUMNS) {
                if (!headerIndex.containsKey(col.toLowerCase())) {
                    errors.add(new ImportErrorDTO(1, col, "缺少必填列: " + col));
                }
            }
            if (!errors.isEmpty()) {
                return new ImportResult(0, errors.size(), errors);
            }

            int rowNum = 1;
            String line;
            while ((line = reader.readLine()) != null) {
                rowNum++;
                if (line.trim().isEmpty()) {
                    continue;
                }

                String[] values = line.split(",");
                List<ImportErrorDTO> rowErrors = new ArrayList<>();

                String name = getValue(values, headerIndex, "name");
                String categoryIdStr = getValue(values, headerIndex, "categoryid");
                String priceStr = getValue(values, headerIndex, "price");
                String stockStr = getValue(values, headerIndex, "stock");
                String subtitle = getValue(values, headerIndex, "subtitle");
                String mainImage = getValue(values, headerIndex, "mainimage");
                String detail = getValue(values, headerIndex, "detail");
                String statusStr = getValue(values, headerIndex, "status");

                if (name == null || name.trim().isEmpty()) {
                    rowErrors.add(new ImportErrorDTO(rowNum, "name", "商品名称不能为空"));
                }
                if (categoryIdStr == null || categoryIdStr.trim().isEmpty()) {
                    rowErrors.add(new ImportErrorDTO(rowNum, "categoryId", "分类ID不能为空"));
                } else {
                    try {
                        Long categoryId = Long.parseLong(categoryIdStr.trim());
                        Category category = categoryMapper.selectById(categoryId);
                        if (category == null) {
                            rowErrors.add(new ImportErrorDTO(rowNum, "categoryId", "分类ID不存在: " + categoryId));
                        }
                    } catch (NumberFormatException e) {
                        rowErrors.add(new ImportErrorDTO(rowNum, "categoryId", "分类ID格式错误"));
                    }
                }
                if (priceStr == null || priceStr.trim().isEmpty()) {
                    rowErrors.add(new ImportErrorDTO(rowNum, "price", "价格不能为空"));
                } else {
                    try {
                        BigDecimal price = new BigDecimal(priceStr.trim());
                        if (price.compareTo(MIN_PRICE) < 0) {
                            rowErrors.add(new ImportErrorDTO(rowNum, "price", "价格不能低于0.01"));
                        }
                    } catch (NumberFormatException e) {
                        rowErrors.add(new ImportErrorDTO(rowNum, "price", "价格格式错误"));
                    }
                }
                if (stockStr == null || stockStr.trim().isEmpty()) {
                    rowErrors.add(new ImportErrorDTO(rowNum, "stock", "库存不能为空"));
                } else {
                    try {
                        Integer.parseInt(stockStr.trim());
                    } catch (NumberFormatException e) {
                        rowErrors.add(new ImportErrorDTO(rowNum, "stock", "库存格式错误"));
                    }
                }

                if (!rowErrors.isEmpty()) {
                    failCount++;
                    errors.addAll(rowErrors);
                    continue;
                }

                try {
                    Product product = new Product();
                    product.setName(name.trim());
                    product.setCategoryId(Long.parseLong(categoryIdStr.trim()));
                    product.setPrice(new BigDecimal(priceStr.trim()));
                    product.setStock(Integer.parseInt(stockStr.trim()));
                    product.setSubtitle(subtitle != null ? subtitle.trim() : null);
                    product.setMainImage(mainImage != null && !mainImage.trim().isEmpty() ? mainImage.trim() : "/images/default-product.svg");
                    product.setDetail(detail != null ? detail.trim() : null);
                    product.setStatus(statusStr != null && !statusStr.trim().isEmpty() ? Integer.parseInt(statusStr.trim()) : 1);
                    product.setSalesCount(0);
                    product.setReviewCount(0);

                    productMapper.insertProduct(product);
                    successCount++;
                } catch (Exception e) {
                    failCount++;
                    errors.add(new ImportErrorDTO(rowNum, "system", "插入失败: " + e.getMessage()));
                    log.error("导入商品失败，行号: {}", rowNum, e);
                }
            }
        } catch (Exception e) {
            errors.add(new ImportErrorDTO(0, "system", "文件读取失败: " + e.getMessage()));
            log.error("读取CSV文件失败", e);
        }

        return new ImportResult(successCount, failCount, errors);
    }

    public void exportProducts(List<Product> products, OutputStream outputStream) {
        try (PrintWriter writer = new PrintWriter(new OutputStreamWriter(outputStream, StandardCharsets.UTF_8))) {
            writer.println('\ufeff' + String.join(",", CSV_HEADERS));

            Map<Long, String> categoryNameMap = new HashMap<>();
            for (Product p : products) {
                String categoryName = categoryNameMap.computeIfAbsent(p.getCategoryId(), id -> {
                    Category cat = categoryMapper.selectById(id);
                    return cat != null ? cat.getName() : "";
                });

                String statusText = p.getStatus() != null && p.getStatus() == 1 ? "上架" : "下架";
                String createdAt = p.getCreatedAt() != null ? p.getCreatedAt().toString() : "";

                writer.println(String.join(",",
                        String.valueOf(p.getId() != null ? p.getId() : ""),
                        escapeCsv(p.getName()),
                        escapeCsv(p.getSubtitle()),
                        String.valueOf(p.getCategoryId() != null ? p.getCategoryId() : ""),
                        escapeCsv(categoryName),
                        String.valueOf(p.getPrice() != null ? p.getPrice() : ""),
                        String.valueOf(p.getStock() != null ? p.getStock() : ""),
                        String.valueOf(p.getSalesCount() != null ? p.getSalesCount() : ""),
                        statusText,
                        createdAt
                ));
            }
            writer.flush();
        } catch (Exception e) {
            log.error("导出CSV失败", e);
            throw new RuntimeException("导出失败: " + e.getMessage());
        }
    }

    private String getValue(String[] values, Map<String, Integer> headerIndex, String key) {
        Integer index = headerIndex.get(key.toLowerCase());
        if (index == null || index >= values.length) {
            return null;
        }
        return values[index];
    }

    private String escapeCsv(String value) {
        if (value == null) {
            return "";
        }
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}
