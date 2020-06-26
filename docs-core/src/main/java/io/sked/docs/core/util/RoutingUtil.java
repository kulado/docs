package io.sked.docs.core.util;

import com.google.common.collect.Lists;
import io.sked.docs.core.constant.AclTargetType;
import io.sked.docs.core.constant.AclType;
import io.sked.docs.core.constant.PermType;
import io.sked.docs.core.dao.AclDao;
import io.sked.docs.core.dao.DocumentDao;
import io.sked.docs.core.dao.RouteModelDao;
import io.sked.docs.core.dao.UserDao;
import io.sked.docs.core.dao.criteria.UserCriteria;
import io.sked.docs.core.dao.dto.RouteStepDto;
import io.sked.docs.core.dao.dto.UserDto;
import io.sked.docs.core.event.DocumentUpdatedAsyncEvent;
import io.sked.docs.core.event.RouteStepValidateEvent;
import io.sked.docs.core.model.context.AppContext;
import io.sked.docs.core.model.jpa.Acl;
import io.sked.docs.core.model.jpa.Document;
import io.sked.docs.core.model.jpa.RouteModel;
import io.sked.util.context.ThreadLocalContext;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonReader;
import java.io.StringReader;
import java.util.List;

/**
 * Routing utilities.
 *
 * @author bgamard
 */
public class RoutingUtil {
    /**
     * Update routing ACLs according to the current route step.
     *
     * @param documentId Document ID
     * @param currentStep Current route step
     * @param previousStep Previous route step
     * @param userId User ID
     */
    public static void updateAcl(String documentId, RouteStepDto currentStep, RouteStepDto previousStep, String userId) {
        AclDao aclDao = new AclDao();

        if (previousStep != null) {
            // Remove the previous ACL
            aclDao.delete(documentId, PermType.READ, previousStep.getTargetId(), userId, AclType.ROUTING);
        }

        if (currentStep != null) {
            // Create a temporary READ ACL
            Acl acl = new Acl();
            acl.setPerm(PermType.READ);
            acl.setType(AclType.ROUTING);
            acl.setSourceId(documentId);
            acl.setTargetId(currentStep.getTargetId());
            aclDao.create(acl, userId);
        }

        // Raise a document updated event
        DocumentUpdatedAsyncEvent event = new DocumentUpdatedAsyncEvent();
        event.setUserId(userId);
        event.setDocumentId(documentId);
        ThreadLocalContext.get().addAsyncEvent(event);
    }

    /**
     * Send an email when a route step is validated.
     *
     * @param documentId Document ID
     * @param routeStepDto Route step DTO
     */
    public static void sendRouteStepEmail(String documentId, RouteStepDto routeStepDto) {
        DocumentDao documentDao = new DocumentDao();
        Document document = documentDao.getById(documentId);

        List<UserDto> userDtoList = Lists.newArrayList();
                UserDao userDao = new UserDao();
        switch (AclTargetType.valueOf(routeStepDto.getTargetType())) {
            case USER:
                userDtoList.addAll(userDao.findByCriteria(new UserCriteria().setUserId(routeStepDto.getTargetId()), null));
                break;
            case GROUP:
                userDtoList.addAll(userDao.findByCriteria(new UserCriteria().setGroupId(routeStepDto.getTargetId()), null));
                break;
        }

        // Fire route step validate events
        for (UserDto userDto : userDtoList) {
            RouteStepValidateEvent routeStepValidateEvent = new RouteStepValidateEvent();
            routeStepValidateEvent.setUser(userDto);
            routeStepValidateEvent.setDocument(document);
            AppContext.getInstance().getMailEventBus().post(routeStepValidateEvent);
        }
    }

    /**
     * Find the first route model name matching a target type and name.
     *
     * @param targetType Target type
     * @param targetName Target name
     * @return Route model name or null if none is matching
     */
    public static String findRouteModelNameByTargetName(AclTargetType targetType, String targetName) {
        RouteModelDao routeModelDao = new RouteModelDao();
        List<RouteModel> routeModelList = routeModelDao.findAll();
        for (RouteModel routeModel : routeModelList) {
            try (JsonReader reader = Json.createReader(new StringReader(routeModel.getSteps()))) {
                JsonArray stepsJson = reader.readArray();
                for (int order = 0; order < stepsJson.size(); order++) {
                    JsonObject step = stepsJson.getJsonObject(order);
                    JsonObject target = step.getJsonObject("target");
                    AclTargetType routeTargetType = AclTargetType.valueOf(target.getString("type"));
                    String routeTargetName = target.getString("name");
                    if (targetType == routeTargetType && targetName.equals(routeTargetName)) {
                        return routeModel.getName();
                    }
                }
            }
        }
        return null;
    }
}
