package com.msop.core.mp.base;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.msop.core.secure.model.MsUser;
import com.msop.core.secure.utils.AuthUtil;
import com.msop.core.tool.constant.MsConstant;
import com.msop.core.tool.utils.*;
import lombok.SneakyThrows;
import org.springframework.transaction.annotation.Transactional;
import javax.validation.constraints.NotEmpty;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

public class BaseServiceImpl<M extends BaseMapper<T>, T extends BaseEntity> extends ServiceImpl<M, T> implements BaseService<T> {
    @Override
    public boolean save(T entity) {
        this.resolveEntity(entity);
        return super.save(entity);
    }

    @Override
    public boolean saveBatch(Collection<T> entityList, int batchSize) {
        entityList.forEach(this::resolveEntity);
        return super.saveBatch(entityList, batchSize);
    }

    @Override
    public boolean updateById(T entity) {
        this.resolveEntity(entity);
        return super.updateById(entity);
    }

    @Override
    public boolean updateBatchById(Collection<T> entityList, int batchSize) {
        entityList.forEach(this::resolveEntity);
        return super.updateBatchById(entityList, batchSize);
    }

    @Override
    public boolean saveOrUpdate(T entity) {
        if (entity.getId() == null) {
            return this.save(entity);
        } else {
            return this.updateById(entity);
        }
    }

    @Override
    public boolean saveOrUpdateBatch(Collection<T> entityList, int batchSize) {
        entityList.forEach(this::resolveEntity);
        return super.saveOrUpdateBatch(entityList, batchSize);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteLogic(@NotEmpty List<Long> ids) {
        MsUser user = AuthUtil.getUser();
        List<T> list = new ArrayList<>();
        ids.forEach(id -> {
            T entity = BeanUtil.newInstance(currentModelClass());
            if (user != null) {
                entity.setUpdateUser(user.getUserId());
            }
            entity.setUpdateTime(DateUtil.now());
            entity.setId(id);
            list.add(entity);
        });
        return super.updateBatchById(list) && super.removeByIds(ids);
    }

    @Override
    public boolean changeStatus(@NotEmpty List<Long> ids, Integer status) {
        MsUser user = AuthUtil.getUser();
        List<T> list = new ArrayList<>();
        ids.forEach(id -> {
            T entity = BeanUtil.newInstance(currentModelClass());
            if (user != null) {
                entity.setUpdateUser(user.getUserId());
            }
            entity.setUpdateTime(DateUtil.now());
            entity.setId(id);
            entity.setStatus(status);
            list.add(entity);
        });
        return super.updateBatchById(list);
    }

    @SneakyThrows
    private void resolveEntity(T entity) {
        MsUser user = AuthUtil.getUser();
        Date now = DateUtil.now();
        if (entity.getId() == null) {
            // ??????????????????
            if (user != null) {
                entity.setCreateUser(user.getUserId());
                entity.setCreateDept(Func.firstLong(user.getDeptId()));
                entity.setUpdateUser(user.getUserId());
            }
            if (entity.getStatus() == null) {
                entity.setStatus(MsConstant.STATUS_NORMAL);
            }
            entity.setCreateTime(now);
        } else if (user != null) {
            // ??????????????????
            entity.setUpdateUser(user.getUserId());
        }
        // ??????????????????
        entity.setUpdateTime(now);
        entity.setIsDeleted(MsConstant.DB_NOT_DELETED);
        // ???????????????????????????????????????????????????????????????
        Field field = ReflectUtil.getField(entity.getClass(), MsConstant.DB_TENANT_KEY);
        if (ObjectUtil.isNotEmpty(field)) {
            Method getTenantId = ClassUtil.getMethod(entity.getClass(), MsConstant.DB_TENANT_KEY_GET_METHOD);
            String tenantId = String.valueOf(getTenantId.invoke(entity));
            if (ObjectUtil.isEmpty(tenantId)) {
                Method setTenantId = ClassUtil.getMethod(entity.getClass(), MsConstant.DB_TENANT_KEY_SET_METHOD, String.class);
                setTenantId.invoke(entity, (Object) null);
            }
        }
    }
}
