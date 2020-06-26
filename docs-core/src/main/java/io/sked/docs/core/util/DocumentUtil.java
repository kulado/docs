package io.sked.docs.core.util;

import io.sked.docs.core.constant.AclType;
import io.sked.docs.core.constant.PermType;
import io.sked.docs.core.dao.AclDao;
import io.sked.docs.core.dao.DocumentDao;
import io.sked.docs.core.model.jpa.Acl;
import io.sked.docs.core.model.jpa.Document;

/**
 * Document utilities.
 *
 * @author bgamard
 */
public class DocumentUtil {
    /**
     * Create a document and add the base ACLs.
     *
     * @param document Document
     * @param userId User creating the document
     * @return Created document
     */
    public static Document createDocument(Document document, String userId) {
        DocumentDao documentDao = new DocumentDao();
        String documentId = documentDao.create(document, userId);

        // Create read ACL
        AclDao aclDao = new AclDao();
        Acl acl = new Acl();
        acl.setPerm(PermType.READ);
        acl.setType(AclType.USER);
        acl.setSourceId(documentId);
        acl.setTargetId(userId);
        aclDao.create(acl, userId);

        // Create write ACL
        acl = new Acl();
        acl.setPerm(PermType.WRITE);
        acl.setType(AclType.USER);
        acl.setSourceId(documentId);
        acl.setTargetId(userId);
        aclDao.create(acl, userId);

        return document;
    }
}
