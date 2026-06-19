package com.shop.service;

import com.shop.dto.ShippingAddressRequest;
import com.shop.entity.ShippingAddress;
import com.shop.mapper.OrderMainMapper;
import com.shop.mapper.ShippingAddressMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ShippingAddressService {

    private final ShippingAddressMapper shippingAddressMapper;
    private final OrderMainMapper orderMainMapper;

    public List<ShippingAddress> listByUserId(Long userId) {
        return shippingAddressMapper.selectByUserId(userId);
    }

    public ShippingAddress getById(Long id, Long userId) {
        ShippingAddress addr = shippingAddressMapper.selectById(id);
        if (addr == null || !addr.getUserId().equals(userId)) {
            return null;
        }
        return addr;
    }

    public ShippingAddress getDefault(Long userId) {
        return shippingAddressMapper.selectDefaultByUserId(userId);
    }

    @Transactional(rollbackFor = Exception.class)
    public ShippingAddress create(Long userId, ShippingAddressRequest req) {
        ShippingAddress addr = new ShippingAddress();
        addr.setUserId(userId);
        addr.setReceiverName(req.getReceiverName());
        addr.setReceiverPhone(req.getReceiverPhone());
        addr.setProvince(req.getProvince());
        addr.setCity(req.getCity());
        addr.setDistrict(req.getDistrict());
        addr.setDetailAddress(req.getDetailAddress());
        addr.setTag(req.getTag());
        if (req.getIsDefault() != null && req.getIsDefault() == 1) {
            shippingAddressMapper.clearDefaultByUserId(userId);
            addr.setIsDefault(1);
        } else {
            List<ShippingAddress> existing = shippingAddressMapper.selectByUserId(userId);
            addr.setIsDefault(existing.isEmpty() ? 1 : 0);
        }
        shippingAddressMapper.insert(addr);
        return shippingAddressMapper.selectById(addr.getId());
    }

    @Transactional(rollbackFor = Exception.class)
    public ShippingAddress update(Long id, Long userId, ShippingAddressRequest req) {
        ShippingAddress addr = shippingAddressMapper.selectById(id);
        if (addr == null || !addr.getUserId().equals(userId)) {
            throw new IllegalArgumentException("地址不存在");
        }
        if (addr.getIsDisabled() != null && addr.getIsDisabled() == 1) {
            throw new IllegalStateException("该地址已被禁用，无法修改");
        }
        addr.setReceiverName(req.getReceiverName());
        addr.setReceiverPhone(req.getReceiverPhone());
        addr.setProvince(req.getProvince());
        addr.setCity(req.getCity());
        addr.setDistrict(req.getDistrict());
        addr.setDetailAddress(req.getDetailAddress());
        addr.setTag(req.getTag());
        if (req.getIsDefault() != null && req.getIsDefault() == 1) {
            shippingAddressMapper.clearDefaultByUserId(userId);
            addr.setIsDefault(1);
        } else {
            addr.setIsDefault(0);
        }
        shippingAddressMapper.update(addr);
        return shippingAddressMapper.selectById(id);
    }

    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id, Long userId) {
        ShippingAddress addr = shippingAddressMapper.selectById(id);
        if (addr == null || !addr.getUserId().equals(userId)) {
            throw new IllegalArgumentException("地址不存在");
        }
        if (addr.getIsDisabled() != null && addr.getIsDisabled() == 1) {
            throw new IllegalStateException("该地址已被禁用，无法删除");
        }
        int pendingCount = orderMainMapper.countPendingByAddressId(id, userId);
        if (pendingCount > 0) {
            throw new IllegalStateException("该地址已被未完成的订单引用，无法删除");
        }
        shippingAddressMapper.deleteById(id);
        if (addr.getIsDefault() != null && addr.getIsDefault() == 1) {
            List<ShippingAddress> remaining = shippingAddressMapper.selectByUserId(userId);
            if (!remaining.isEmpty()) {
                shippingAddressMapper.updateIsDefault(remaining.get(0).getId(), 1);
            }
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void setDefault(Long id, Long userId) {
        ShippingAddress addr = shippingAddressMapper.selectById(id);
        if (addr == null || !addr.getUserId().equals(userId)) {
            throw new IllegalArgumentException("地址不存在");
        }
        shippingAddressMapper.clearDefaultByUserId(userId);
        shippingAddressMapper.updateIsDefault(id, 1);
    }
}
