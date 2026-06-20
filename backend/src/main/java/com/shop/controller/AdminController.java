package com.shop.controller;

import com.shop.common.Result;
import com.shop.dto.AdminProductQueryDTO;
import com.shop.dto.BatchOperationRequest;
import com.shop.dto.BatchOperationResult;
import com.shop.dto.ImportResult;
import com.shop.dto.PageResult;
import com.shop.dto.ReviewReplyRequest;
import com.shop.dto.ReviewVO;
import com.shop.dto.ShipRequest;
import com.shop.dto.UserVO;
import com.shop.entity.Category;
import com.shop.entity.OrderMain;
import com.shop.entity.OrderItem;
import com.shop.entity.Product;
import com.shop.entity.ProductImage;
import com.shop.service.AdminService;
import com.shop.service.CategoryService;
import com.shop.service.PriceAlertService;
import com.shop.service.ProductImageService;
import com.shop.service.ProductService;
import com.shop.mapper.ProductMapper;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;
    private final ProductImageService productImageService;
    private final PriceAlertService priceAlertService;
    private final ProductMapper productMapper;
    private final ProductService productService;
    private final CategoryService categoryService;

    private Long requireAdminId(Authentication auth) {
        if (auth == null || !(auth.getPrincipal() instanceof Long)) {
            throw new IllegalStateException("未登录");
        }
        return (Long) auth.getPrincipal();
    }

    @GetMapping("/users")
    public Result<List<UserVO>> listUsers() {
        return Result.ok(adminService.listUsers());
    }

    @PutMapping("/users/{id}")
    public Result<Void> updateUser(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        Integer status = body.get("status") != null ? ((Number) body.get("status")).intValue() : null;
        String role = body.get("role") != null ? String.valueOf(body.get("role")) : null;
        adminService.updateUser(id, status, role);
        return Result.ok();
    }

    @GetMapping("/reviews")
    public Result<List<ReviewVO>> listReviews() {
        return Result.ok(adminService.listAllReviews());
    }

    @DeleteMapping("/reviews/{id}")
    public Result<Void> deleteReview(@PathVariable Long id) {
        adminService.deleteReview(id);
        return Result.ok();
    }

    @PostMapping("/reviews/reply")
    public Result<Void> replyReview(Authentication auth, @Valid @RequestBody ReviewReplyRequest req) {
        Long adminId = requireAdminId(auth);
        String adminName = auth.getName() != null ? auth.getName() : null;
        adminService.replyReview(adminId, adminName, req);
        return Result.ok();
    }

    @GetMapping("/orders")
    public Result<List<OrderMain>> listOrders(@RequestParam(required = false) Integer status) {
        return Result.ok(adminService.listOrdersByStatus(status));
    }

    @GetMapping("/orders/pending-ship")
    public Result<List<OrderMain>> listPendingShipOrders() {
        return Result.ok(adminService.listOrdersByStatus(1));
    }

    @GetMapping("/orders/{id}")
    public Result<OrderMain> getOrder(@PathVariable Long id) {
        OrderMain order = adminService.getOrderById(id);
        if (order == null) {
            return Result.fail(404, "订单不存在");
        }
        return Result.ok(order);
    }

    @GetMapping("/orders/{id}/items")
    public Result<List<OrderItem>> getOrderItems(@PathVariable Long id) {
        return Result.ok(adminService.listOrderItems(id));
    }

    @PostMapping("/orders/ship")
    public Result<Void> ship(Authentication auth, @RequestBody ShipRequest req) {
        Long adminId = requireAdminId(auth);
        String adminName = auth.getName() != null ? auth.getName() : null;
        adminService.ship(req, adminId, adminName);
        return Result.ok();
    }

    @GetMapping("/products/{productId}/images")
    public Result<List<ProductImage>> listProductImages(@PathVariable Long productId) {
        return Result.ok(productImageService.listByProductId(productId));
    }

    @PostMapping("/products/{productId}/images")
    public Result<ProductImage> addProductImage(@PathVariable Long productId, @RequestBody Map<String, String> body) {
        String imageUrl = body.get("imageUrl");
        if (imageUrl == null || imageUrl.trim().isEmpty()) {
            return Result.fail("图片URL不能为空");
        }
        Integer sortOrder = body.get("sortOrder") != null ? Integer.valueOf(body.get("sortOrder")) : null;
        return Result.ok(productImageService.addImage(productId, imageUrl.trim(), sortOrder));
    }

    @PostMapping("/products/{productId}/images/bulk")
    public Result<List<ProductImage>> bulkImportImages(@PathVariable Long productId, @RequestBody Map<String, List<String>> body) {
        List<String> urls = body.get("urls");
        if (urls == null || urls.isEmpty()) {
            return Result.fail("URL列表不能为空");
        }
        return Result.ok(productImageService.bulkImport(productId, urls));
    }

    @DeleteMapping("/products/images/{id}")
    public Result<Void> deleteProductImage(@PathVariable Long id) {
        productImageService.deleteImage(id);
        return Result.ok();
    }

    @PutMapping("/products/images/{id}/set-main")
    public Result<Void> setMainImage(@PathVariable Long id) {
        productImageService.setMainImage(id);
        return Result.ok();
    }

    @PutMapping("/products/images/reorder")
    public Result<Void> reorderImages(@RequestBody Map<String, Object> body) {
        Long productId = ((Number) body.get("productId")).longValue();
        @SuppressWarnings("unchecked")
        List<Long> orderedIds = ((List<Number>) body.get("orderedIds")).stream()
                .map(Number::longValue).toList();
        productImageService.reorder(productId, orderedIds);
        return Result.ok();
    }

    @PutMapping("/products/{id}/price")
    public Result<Void> updateProductPrice(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        java.math.BigDecimal newPrice = new java.math.BigDecimal(body.get("price").toString());
        if (newPrice.compareTo(java.math.BigDecimal.ZERO) <= 0) {
            return Result.fail("价格必须大于0");
        }
        productMapper.updatePrice(id, newPrice);
        priceAlertService.checkAndNotify(id);
        return Result.ok();
    }

    @GetMapping("/products")
    public Result<PageResult<Product>> listProducts(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        AdminProductQueryDTO query = new AdminProductQueryDTO();
        query.setName(name);
        query.setCategoryId(categoryId);
        query.setStatus(status);
        query.setMinPrice(minPrice);
        query.setMaxPrice(maxPrice);
        query.setPageNum(pageNum);
        query.setPageSize(pageSize);
        return Result.ok(productService.adminList(query));
    }

    @GetMapping("/categories")
    public Result<List<Category>> listCategories() {
        return Result.ok(categoryService.listAll());
    }

    @PostMapping("/products/batch/status")
    public Result<BatchOperationResult> batchUpdateStatus(@RequestBody BatchOperationRequest request) {
        if (request.getProductIds() == null || request.getProductIds().isEmpty()) {
            return Result.fail("请选择商品");
        }
        if (request.getStatus() == null) {
            return Result.fail("状态不能为空");
        }
        return Result.ok(adminService.batchUpdateStatus(request.getProductIds(), request.getStatus()));
    }

    @PostMapping("/products/batch/category")
    public Result<BatchOperationResult> batchUpdateCategory(@RequestBody BatchOperationRequest request) {
        if (request.getProductIds() == null || request.getProductIds().isEmpty()) {
            return Result.fail("请选择商品");
        }
        if (request.getCategoryId() == null) {
            return Result.fail("分类ID不能为空");
        }
        return Result.ok(adminService.batchUpdateCategory(request.getProductIds(), request.getCategoryId()));
    }

    @PostMapping("/products/batch/price")
    public Result<BatchOperationResult> batchUpdatePrice(@RequestBody BatchOperationRequest request) {
        if (request.getProductIds() == null || request.getProductIds().isEmpty()) {
            return Result.fail("请选择商品");
        }
        if (request.getPriceRatio() == null) {
            return Result.fail("调价比例不能为空");
        }
        return Result.ok(adminService.batchUpdatePrice(request.getProductIds(), request.getPriceRatio()));
    }

    @PostMapping("/products/import")
    public Result<ImportResult> importProducts(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return Result.fail("请选择文件");
        }
        String filename = file.getOriginalFilename();
        if (filename == null || !filename.toLowerCase().endsWith(".csv")) {
            return Result.fail("仅支持CSV文件");
        }
        try {
            ImportResult result = adminService.importProducts(file.getInputStream());
            return Result.ok(result);
        } catch (IOException e) {
            return Result.fail("文件读取失败: " + e.getMessage());
        }
    }

    @GetMapping("/products/export")
    public void exportProducts(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            HttpServletResponse response) throws IOException {
        AdminProductQueryDTO query = new AdminProductQueryDTO();
        query.setName(name);
        query.setCategoryId(categoryId);
        query.setStatus(status);
        query.setMinPrice(minPrice);
        query.setMaxPrice(maxPrice);

        List<Product> products = productService.adminListForExport(query);

        response.setContentType("text/csv;charset=utf-8");
        String filename = URLEncoder.encode("商品列表_" + System.currentTimeMillis() + ".csv", StandardCharsets.UTF_8);
        response.setHeader("Content-Disposition", "attachment; filename*=UTF-8''" + filename);

        adminService.exportProducts(products, response.getOutputStream());
    }
}
