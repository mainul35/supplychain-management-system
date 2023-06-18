package com.mainul35.bsuserinfo.controllers;

import com.mainul35.bsuserinfo.entity.UserConnection;
import com.mainul35.bsuserinfo.entity.UserEntity;
import com.mainul35.bsuserinfo.services.definition.UserConnectionService;
import controllers.definition.IConnectionController;
import controllers.dtos.response.UserConnectionInfoResponse;
import controllers.dtos.response.UserInfoResponse;
import org.springframework.beans.BeanUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

@RestController
public class UserConnectionController implements IConnectionController {

    private final UserConnectionService userConnectionService;
    public UserConnectionController(UserConnectionService userConnectionService1) {
        this.userConnectionService = userConnectionService1;
    }
    @Override
    public ResponseEntity<UserConnectionInfoResponse> requestConnection(String userId, String connectionId) {
        var userConnection = userConnectionService.addConnection(userId, connectionId);
        UserConnectionInfoResponse response = mapUserConnectionEntityToDto(userConnection);
        return ResponseEntity.ok(response);
    }

    private UserConnectionInfoResponse mapUserConnectionEntityToDto(UserConnection userConnection) {
        UserConnectionInfoResponse response = new UserConnectionInfoResponse();
        BeanUtils.copyProperties(userConnection, response);
        BeanUtils.copyProperties(userConnection.getUserConnectionId().getUser(), response.getUser());
        BeanUtils.copyProperties(userConnection.getUserConnectionId().getConnection(), response.getConnection());
        response.setStatus(userConnection.getConnectionStatus());
        return response;
    }

    @Override
    public ResponseEntity<UserConnectionInfoResponse> acceptConnection(String userId, String connectionId) {
        var acceptedUserConnection = userConnectionService.acceptConnection(userId, connectionId);
        return ResponseEntity.ok(acceptedUserConnection);
    }

    @Override
    public ResponseEntity<UserConnectionInfoResponse> rejectConnection(String userId, String connectionId) {
        var rejectedConnection = userConnectionService.rejectConnection(userId, connectionId);
        return ResponseEntity.ok(rejectedConnection);
    }

    @Override
    public ResponseEntity<UserConnectionInfoResponse> blockConnection(String userId, String connectionId) {
        var blockedConnection = userConnectionService.blockConnection(userId, connectionId);
        return ResponseEntity.ok(blockedConnection);
    }

    @Override
    public ResponseEntity<UserConnectionInfoResponse> unblockConnection(String userId, String connectionId) {
        var unblockedConnection = userConnectionService.unblockConnection(userId, connectionId);
        return ResponseEntity.ok(unblockedConnection);
    }

    @Override
    public ResponseEntity<List<UserConnectionInfoResponse>> getConnectionRequests(String userId, Integer pageIxd, Integer itemsPerPage) {
        var list = userConnectionService.getAllConnectionRequests(userId, pageIxd, itemsPerPage);
        return ResponseEntity.ok(list);
    }

    @Override
    public ResponseEntity<List<UserConnectionInfoResponse>> getBlockedConnections(String userId, Integer pageIxd, Integer itemsPerPage) {
        var list = userConnectionService.getAllBlockedConnections(userId, pageIxd, itemsPerPage);
        return ResponseEntity.ok(list);
    }

    @Override
    public ResponseEntity<List<UserConnectionInfoResponse>> getConnectedUsers(String userId, Integer pageIxd, Integer itemsPerPage) {
        var list = userConnectionService.getAllAcceptedConnections(userId, pageIxd, itemsPerPage);
        return ResponseEntity.ok(list);
    }

    private List<UserInfoResponse> convertUserEntityListToUserInfoResponseList(List<UserEntity> userEntityList) {
        var userInfoResponses = new ArrayList<UserInfoResponse>();
        userEntityList.forEach(userEntity -> {
            UserInfoResponse response = new UserInfoResponse();
            BeanUtils.copyProperties(userEntity, response);
            userInfoResponses.add(response);
        });
        return userInfoResponses;
    }
}
