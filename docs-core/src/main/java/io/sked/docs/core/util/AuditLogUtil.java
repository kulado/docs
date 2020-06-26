package io.sked.docs.core.util;

import io.sked.docs.core.constant.AuditLogType;
import io.sked.docs.core.dao.AuditLogDao;
import io.sked.docs.core.model.jpa.AuditLog;
import io.sked.docs.core.model.jpa.Loggable;
import io.sked.util.context.ThreadLocalContext;

import javax.persistence.EntityManager;

/**
 * Audit log utilities.
 * 
 * @author bgamard
 */
public class AuditLogUtil {
    /**
     * Create an audit log.
     * 
     * @param loggable Loggable
     * @param type Audit log type
     * @param userId User ID
     */
    public static void create(Loggable loggable, AuditLogType type, String userId) {
        if (userId == null) {
            userId = "admin";
        }

        // Get the entity ID
        EntityManager em = ThreadLocalContext.get().getEntityManager();
        String entityId = (String) em.getEntityManagerFactory().getPersistenceUnitUtil().getIdentifier(loggable);
        
        // Create the audit log
        AuditLogDao auditLogDao = new AuditLogDao();
        AuditLog auditLog = new AuditLog();
        auditLog.setUserId(userId);
        auditLog.setEntityId(entityId);
        auditLog.setEntityClass(loggable.getClass().getSimpleName());
        auditLog.setType(type);
        auditLog.setMessage(loggable.toMessage());
        auditLogDao.create(auditLog);
    }
}
