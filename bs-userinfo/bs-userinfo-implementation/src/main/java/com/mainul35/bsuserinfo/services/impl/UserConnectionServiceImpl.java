package com.mainul35.bsuserinfo.services.impl;

import controllers.dtos.enums.ConnectionStatus;
import com.mainul35.bsuserinfo.entity.UserConnection;
import com.mainul35.bsuserinfo.entity.UserConnectionId;
import com.mainul35.bsuserinfo.entity.UserEntity;
import com.mainul35.bsuserinfo.exceptions.NoContentException;
import com.mainul35.bsuserinfo.repositories.UserConnectionRepository;
import com.mainul35.bsuserinfo.repositories.UserInfoRepository;
import com.mainul35.bsuserinfo.services.definition.UserConnectionService;
import controllers.dtos.response.UserConnectionInfoResponse;
import controllers.dtos.response.UserInfoResponse;
import org.springframework.beans.BeanUtils;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

@Service
@Transactional
public class UserConnectionServiceImpl implements UserConnectionService {

    private final UserInfoRepository userInfoRepository;
    private final UserConnectionRepository connectionRepository;
    UserConnectionServiceImpl(UserInfoRepository userInfoRepository,
                              UserConnectionRepository connectionRepository) {
        this.userInfoRepository = userInfoRepository;
        this.connectionRepository = connectionRepository;
    }
    @Transactional
    @Override
    public UserConnection addConnection(String username, String connectionUsername) {
        var userConnection = connectionRepository.findByUserConnectionId(getUserConnectionId(username, connectionUsername))
                .orElse(connectionRepository.findByUserConnectionId(getUserConnectionId(connectionUsername, username))
                        .orElse(new UserConnection(getUserConnectionId(username, connectionUsername), ConnectionStatus.REQUESTED)));
        userConnection.setConnectionStatus(ConnectionStatus.REQUESTED);
        userConnection.setRequestedById(username);
        return connectionRepository.save(userConnection);
    }

    private UserConnectionId getUserConnectionId(String username, String connectionUsername) {
        var user = userInfoRepository.findByUsername(username)
                .orElseThrow(() -> new NoContentException("No user found with this username"));
        var connection = userInfoRepository.findByUsername(connectionUsername)
                .orElseThrow(() -> new NoContentException("No user found to connect"));
        return new UserConnectionId(user, connection);
    }

    @Override
    public UserConnectionInfoResponse acceptConnection(String username, String connectionUsername) {

        var userConnection = connectionRepository.findByUserConnectionId(getUserConnectionId(username, connectionUsername))
                .orElse(connectionRepository.findByUserConnectionId(getUserConnectionId(connectionUsername, username)).orElse(null));
        if (Objects.isNull(userConnection)) {
            throw new NoContentException("No such connection request found");
        }
        userConnection.setConnectionStatus(ConnectionStatus.ACCEPTED);
        userConnection.setRequestedById(null);
        userConnection.setBlockedById(null);
        return prepareUserConnectionResponse(connectionRepository.save(userConnection), username) ;
    }

    @Override
    public UserConnectionInfoResponse rejectConnection(String username, String connectionUsername) {
        var userConnection = connectionRepository.findByUserConnectionId(getUserConnectionId(username, connectionUsername))
                .orElse(connectionRepository.findByUserConnectionId(getUserConnectionId(connectionUsername, username)).orElse(null));
        if (Objects.isNull(userConnection)) {
            throw new NoContentException("No such connection request found");
        }
        userConnection.setConnectionStatus(ConnectionStatus.REJECTED);
        userConnection.setRequestedById(null);
        userConnection.setBlockedById(null);
        return prepareUserConnectionResponse(connectionRepository.save(userConnection), username);
    }

    @Override
    public UserConnectionInfoResponse blockConnection(String username, String connectionUsername) {
        var userConnection = connectionRepository.findByUserConnectionId(getUserConnectionId(username, connectionUsername))
                .orElse(connectionRepository.findByUserConnectionId(getUserConnectionId(connectionUsername, username)).orElse(null));
        if (Objects.isNull(userConnection)) {
            throw new NoContentException("No such connection request found");
        }
        userConnection.setConnectionStatus(ConnectionStatus.BLOCKED);
        userConnection.setRequestedById(null);
        userConnection.setBlockedById(username);
        return prepareUserConnectionResponse(connectionRepository.save(userConnection), username);
    }

    @Override
    public UserConnectionInfoResponse unblockConnection(String username, String connectionUsername) {
        var userConnection = connectionRepository.findByUserConnectionId(getUserConnectionId(username, connectionUsername))
                .orElse(connectionRepository.findByUserConnectionId(getUserConnectionId(connectionUsername, username)).orElse(null));
        if (Objects.isNull(userConnection)) {
            throw new NoContentException("No such connection request found");
        }
        userConnection.setConnectionStatus(ConnectionStatus.UNBLOCKED);
        userConnection.setRequestedById(null);
        userConnection.setBlockedById(null);
        return prepareUserConnectionResponse(connectionRepository.save(userConnection), username);
    }

    @Override
    public List<UserConnectionInfoResponse> getAllConnectionRequests(String username, Integer pageIxd, Integer itemsPerPage) {
        var user = userInfoRepository.findByUsername(username)
                .orElseThrow(() -> new NoContentException("No user found with this user Id"));
        var stream1 = connectionRepository
                .findAllByUserConnectionId_UserAndConnectionStatus(user, ConnectionStatus.REQUESTED);
        var stream2 = connectionRepository.findAllByUserConnectionId_ConnectionAndConnectionStatus(user, ConnectionStatus.REQUESTED);
        return Stream.concat(stream1, stream2).parallel()
                .map(userConnection -> populateUserConnectionInfoResponseFromUserEntity(userConnection, user))
                .filter(userConnection -> !username.equals(userConnection.getRequestedById()))
                .skip((long) (pageIxd - 1) * itemsPerPage).limit(itemsPerPage).toList();
    }

    @Override
    public List<UserConnectionInfoResponse> getAllBlockedConnections(String username, Integer pageIxd, Integer itemsPerPage) {
        var user = userInfoRepository.findByUsername(username)
                .orElseThrow(() -> new NoContentException("No user found with this user Id"));
        var stream1 = connectionRepository
                .findAllByUserConnectionId_UserAndConnectionStatus(user, ConnectionStatus.BLOCKED);
        var stream2 = connectionRepository.findAllByUserConnectionId_ConnectionAndConnectionStatus(user, ConnectionStatus.BLOCKED);
        return Stream.concat(stream1, stream2)
                .map(userConnection -> populateUserConnectionInfoResponseFromUserEntity(userConnection, user))
                .filter(userConnection -> username.equals(userConnection.getBlockedById()))
                .skip((long) (pageIxd - 1) * itemsPerPage).limit(itemsPerPage).toList();
    }

    @Override
    public List<UserConnectionInfoResponse> getAllAcceptedConnections(String username, Integer pageIxd, Integer itemsPerPage) {
        var user = userInfoRepository.findByUsername(username)
                .orElseThrow(() -> new NoContentException("No user found with this username"));
        var stream1 = connectionRepository
                .findAllByUserConnectionId_UserAndConnectionStatus(user, ConnectionStatus.ACCEPTED);
        var stream2 = connectionRepository.findAllByUserConnectionId_ConnectionAndConnectionStatus(user, ConnectionStatus.ACCEPTED);
        return Stream.concat(stream1, stream2)
                .map(userConnection -> populateUserConnectionInfoResponseFromUserEntity(userConnection, user))
                .filter(userConnection -> !username.equals(userConnection.getRequestedById()))
                .skip((long) (pageIxd - 1) * itemsPerPage).limit(itemsPerPage).toList();
    }

    @Deprecated
    private List<UserEntity> populatedUserEntitiesFromConnectionList(List<UserConnection> list) {
        List<UserEntity> userEntities = new ArrayList<>();
        list.forEach(userConnection -> {
            var connection = userInfoRepository.findById(userConnection.getUserConnectionId().getConnection().getId());
            connection.ifPresent(userEntities::add);
        });
        return userEntities;
    }
    @Deprecated
    private PageRequest sortAndPaginate(Integer pageIxd, Integer itemsPerPage) {
        return PageRequest.of(pageIxd - 1, itemsPerPage);
    }

    /**
     * Will be used to return user suggestions
     * */
    @Transactional
    @Override
    public List<UserConnectionInfoResponse> getNonConnectedUsers(String username, Integer pageIxd, Integer itemsPerPage) {
        var userOptional = userInfoRepository.findByUsername(username);
        if(userOptional.isPresent()){
            var stream1 = connectionRepository.findByUserConnectionId_User(userOptional.get());
            var stream2 = connectionRepository.findByUserConnectionId_Connection(userOptional.get());
            return Stream.concat(stream1, stream2).parallel()
                    .filter(userConnection -> List.of(ConnectionStatus.UNBLOCKED, ConnectionStatus.NEW,ConnectionStatus.REJECTED)
                            .contains(userConnection.getConnectionStatus())
                    ).map(userConnection -> {
                        var userEntity = userOptional.get();
                        return populateUserConnectionInfoResponseFromUserEntity(userConnection, userEntity);
                    })
                    .skip((long) (pageIxd - 1) * itemsPerPage).limit(itemsPerPage).toList();
        }
        return List.of(new UserConnectionInfoResponse());
    }

    private UserConnectionInfoResponse populateUserConnectionInfoResponseFromUserEntity(UserConnection userConnection, UserEntity userEntity) {
        return prepareUserConnectionResponse(userConnection, userEntity.getUsername());
    }

    private UserConnectionInfoResponse prepareUserConnectionResponse(UserConnection userConnection, String username) {
        UserConnectionInfoResponse response = new UserConnectionInfoResponse();
        if(userConnection.getUserConnectionId().getUser().getUsername().equals(username)) {
            response.setUser(this.mapUserEntityToResponseDto(userConnection.getUserConnectionId().getUser()));
            response.setConnection(this.mapUserEntityToResponseDto(userConnection.getUserConnectionId().getConnection()));
        } else {
            response.setConnection(this.mapUserEntityToResponseDto(userConnection.getUserConnectionId().getUser()));
            response.setUser(this.mapUserEntityToResponseDto(userConnection.getUserConnectionId().getConnection()));
        }
        response.setStatus(userConnection.getConnectionStatus());
        response.setRequestedById(userConnection.getRequestedById());
        response.setBlockedById(userConnection.getBlockedById());
        return response;
    }

    private UserInfoResponse mapUserEntityToResponseDto(UserEntity user) {
        var userInfoResp = new UserInfoResponse();
        BeanUtils.copyProperties(user, userInfoResp);
        return userInfoResp;
    }
}

/**
 * verbose Code
 * */

/*
@Service
public class UserConnectionService {

    private final UserInfoRepository userInfoRepository;
    private final UserConnectionRepository connectionRepository;

    public UserConnectionService(UserInfoRepository userInfoRepository, UserConnectionRepository connectionRepository) {
        this.userInfoRepository = userInfoRepository;
        this.connectionRepository = connectionRepository;
    }

    public void addConnection(String userId, String connectionId) {
        var userConnection = new UserConnection();
        UserConnectionId userConnectionId = getUserConnectionId(userId, connectionId);
        userConnection.setUserConnectionId(userConnectionId);
        userConnection.setConnectionStatus(ConnectionStatus.REQUESTED);
        connectionRepository.save(userConnection);
    }

    private UserConnectionId getUserConnectionId(String userId, String connectionId) {
        var userConnectionId = new UserConnectionId();
        var user = userInfoRepository.findById(userId)
                .orElseThrow(() -> new NoContentException("No user found with this userId"));
        var connection = userInfoRepository.findById(connectionId)
                .orElseThrow(() -> new NoContentException("No user found to connect"));
        userConnectionId.setUser(user);
        userConnectionId.setConnection(connection);
        return userConnectionId;
    }

    public void acceptConnection(String userId, String connectionId) {
        var userConnectionId = getUserConnectionId(userId, connectionId);
        var userConnection = connectionRepository.findByUserConnectionId(userConnectionId)
                .orElse(connectionRepository.findByUserConnectionId(getUserConnectionId(connectionId, userId))
                        .orElse(null));
        if (Objects.isNull(userConnection)) {
            throw new NoContentException("No such connection request found");
        }
        userConnection.setConnectionStatus(ConnectionStatus.ACCEPTED);
        connectionRepository.save(userConnection);
    }

    public void rejectConnection(String userId, String connectionId) {
        var userConnectionId = getUserConnectionId(userId, connectionId);
        var userConnection = connectionRepository.findByUserConnectionId(userConnectionId)
                .orElse(connectionRepository.findByUserConnectionId(getUserConnectionId(connectionId, userId)).orElse(null));
        if (Objects.isNull(userConnection)) {
            throw new NoContentException("No such connection request found");
        }
        userConnection.setConnectionStatus(ConnectionStatus.REJECTED);
        connectionRepository.save(userConnection);
    }

    public void blockConnection(String userId, String connectionId) {
        var userConnectionId = getUserConnectionId(userId, connectionId);
        var userConnection = connectionRepository.findByUserConnectionId(userConnectionId)
                .orElse(connectionRepository.findByUserConnectionId(getUserConnectionId(connectionId, userId)).orElse(null));
        if (Objects.isNull(userConnection)) {
            throw new NoContentException("No such connection request found");
        }
        userConnection.setConnectionStatus(ConnectionStatus.BLOCKED);
        connectionRepository.save(userConnection);
    }

    public void unblockConnection(String userId, String connectionId) {
        var userConnectionId = getUserConnectionId(userId, connectionId);
        var userConnection = connectionRepository.findByUserConnectionId(userConnectionId)
                .orElse(connectionRepository.findByUserConnectionId(getUserConnectionId(connectionId, userId)).orElse(null));
        if (Objects.isNull(userConnection)) {
            throw new NoContentException("No such connection request found");
        }
        userConnection.setConnectionStatus(ConnectionStatus.UNBLOCKED);
        connectionRepository.save(userConnection);
    }

    public List<UserEntity> getAllConnectionRequests(String userId, Integer pageIxd, Integer itemsPerPage) {
        var user = userInfoRepository.findById(userId).orElseThrow(() -> new NoContentException("No user found with this user Id"));
        var list = connectionRepository.findAllByUserConnectionId_UserAndAndConnectionStatus_Requested(user, PageRequest.of(pageIxd - 1, itemsPerPage, Sort.by(Sort.Order.asc("username"))));
        List<UserEntity> userEntities = new ArrayList<>();
        list.forEach(userConnection -> {
            var connection = userInfoRepository.findById(userConnection.getUserConnectionId().getConnection().getId());
            connection.ifPresent(userEntities::add);
        });
        return userEntities;
    }

    public List<UserEntity> getAllBlockedConnections(String userId, Integer pageIxd, Integer itemsPerPage) {
        var user = userInfoRepository.findById(userId);
        user.orElseThrow(() -> new NoContentException("No user found with this user Id"));
        var pagable = PageRequest.of(pageIxd - 1, itemsPerPage, Sort.by(Sort.Order.asc("username")));
        var list = connectionRepository.findAllByUserConnectionId_UserAndAndConnectionStatus_Blocked(user.get(), pagable);
        List<UserEntity> userEntities = new ArrayList<>();
        list.forEach(userConnection -> {
            var connection = userInfoRepository.findById(userConnection.getUserConnectionId().getConnection().getId());
            connection.ifPresent(userEntities::add);
        });
        return userEntities;
    }
}*/
