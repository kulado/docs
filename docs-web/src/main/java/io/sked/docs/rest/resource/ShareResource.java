package io.sked.docs.rest.resource;


import io.sked.docs.core.constant.AclTargetType;
import io.sked.docs.core.constant.AclType;
import io.sked.docs.core.constant.PermType;
import io.sked.docs.core.dao.AclDao;
import io.sked.docs.core.dao.ShareDao;
import io.sked.docs.core.event.DocumentUpdatedAsyncEvent;
import io.sked.docs.core.model.jpa.Acl;
import io.sked.docs.core.model.jpa.Share;
import io.sked.rest.exception.ClientException;
import io.sked.rest.exception.ForbiddenClientException;
import io.sked.rest.util.ValidationUtil;
import io.sked.util.JsonUtil;
import io.sked.util.context.ThreadLocalContext;

import javax.json.Json;
import javax.json.JsonObjectBuilder;
import javax.ws.rs.*;
import javax.ws.rs.core.Response;
import java.text.MessageFormat;
import java.util.List;

/**
 * Share REST resources.
 * 
 * @author bgamard
 */
@Path("/share")
public class ShareResource extends BaseResource {
    /**
     * Add a share to a document.
     *
     * @api {put} /share Share a document
     * @apiName PutShare
     * @apiGroup Share
     * @apiParam {String} id Document ID
     * @apiParam {String} name Share name
     * @apiSuccess {String} id Acl ID
     * @apiSuccess {String="READ","WRITE"} perm Permission
     * @apiSuccess {String} name Share name
     * @apiSuccess {String="SHARE"} type ACL type (always SHARE)
     * @apiError (client) ForbiddenError Access denied
     * @apiError (client) ValidationError Validation error
     * @apiError (client) NotFound Share not found
     * @apiPermission user
     * @apiVersion 1.5.0
     *
     * @param documentId Document ID
     * @param name Share name
     * @return Response
     */
    @PUT
    public Response add(
            @FormParam("id") String documentId,
            @FormParam("name") String name) {
        if (!authenticate()) {
            throw new ForbiddenClientException();
        }

        // Validate input data
        ValidationUtil.validateRequired(documentId, "id");
        name = ValidationUtil.validateLength(name, "name", 1, 36, true);

        // Check write permission on the document
        AclDao aclDao = new AclDao();
        if (!aclDao.checkPermission(documentId, PermType.WRITE, getTargetIdList(null))) {
            throw new NotFoundException();
        }
        
        // Create the share
        ShareDao shareDao = new ShareDao();
        Share share = new Share();
        share.setName(name);
        shareDao.create(share);
        
        // Create the ACL
        Acl acl = new Acl();
        acl.setSourceId(documentId);
        acl.setPerm(PermType.READ);
        acl.setType(AclType.USER);
        acl.setTargetId(share.getId());
        aclDao.create(acl, principal.getId());

        // Raise a document updated event
        DocumentUpdatedAsyncEvent event = new DocumentUpdatedAsyncEvent();
        event.setUserId(principal.getId());
        event.setDocumentId(documentId);
        ThreadLocalContext.get().addAsyncEvent(event);

        // Returns the created ACL
        JsonObjectBuilder response = Json.createObjectBuilder()
                .add("perm", acl.getPerm().name())
                .add("id", acl.getTargetId())
                .add("name", JsonUtil.nullable(name))
                .add("type", AclTargetType.SHARE.toString());
        return Response.ok().entity(response.build()).build();
    }

    /**
     * Deletes a share.
     *
     * @api {delete} /share/:id Unshare a document
     * @apiName DeleteShare
     * @apiGroup Share
     * @apiParam {String} id Acl ID
     * @apiSuccess {String} status Status OK
     * @apiError (client) ForbiddenError Access denied
     * @apiError (client) ShareNotFound Share not found
     * @apiError (client) DocumentNotFound Document not found
     * @apiPermission user
     * @apiVersion 1.5.0
     *
     * @param id Share ID
     * @return Response
     */
    @DELETE
    @Path("{id: [a-z0-9\\-]+}")
    public Response delete(
            @PathParam("id") String id) {
        if (!authenticate()) {
            throw new ForbiddenClientException();
        }

        // Check that the user can share the linked document
        AclDao aclDao = new AclDao();
        List<Acl> aclList = aclDao.getByTargetId(id);
        if (aclList.isEmpty()) {
            throw new ClientException("ShareNotFound", MessageFormat.format("Share not found: {0}", id));
        }

        Acl acl = aclList.get(0);
        if (!aclDao.checkPermission(acl.getSourceId(), PermType.WRITE, getTargetIdList(null))) {
            throw new ClientException("DocumentNotFound", MessageFormat.format("Document not found: {0}", acl.getSourceId()));
        }

        // Delete the share
        ShareDao shareDao = new ShareDao();
        shareDao.delete(id);

        // Raise a document updated event
        DocumentUpdatedAsyncEvent event = new DocumentUpdatedAsyncEvent();
        event.setUserId(principal.getId());
        event.setDocumentId(acl.getSourceId());
        ThreadLocalContext.get().addAsyncEvent(event);
        
        // Always return OK
        JsonObjectBuilder response = Json.createObjectBuilder()
                .add("status", "ok");
        return Response.ok().entity(response.build()).build();
    }
}
