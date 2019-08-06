package com.sequenceiq.freeipa.service.freeipa.user.model;

import static java.util.Objects.requireNonNull;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetRightsResponse;
import com.cloudera.thunderhead.service.usermanagement.UserManagementProto.Group;
import com.cloudera.thunderhead.service.usermanagement.UserManagementProto.MachineUser;
import com.cloudera.thunderhead.service.usermanagement.UserManagementProto.User;

public class UmsState {
    private Map<String, Group> groupMap;

    // Regular users
    private Map<String, User> userMap;

//    private Map<String, GetRightsResponse> userRightsMap;

    // Admin Users for an environment.
    private Map<String, User> adminUserMap;

    private Map<String, GetRightsResponse> adminUserRightsMap = new HashMap<>();

    private Map<String, MachineUser> adminMachineUserMap = new HashMap<>();

    private Map<String, MachineUser> machineUserMap;

//    private Map<String, GetRightsResponse> machineUserRightsMap;

    public UmsState(Map<String, Group> groupMap, Map<String, User> adminUserMap,
                    Map<String, MachineUser> adminMachineUserMap, Map<String, User> userMap,
                    Map<String, MachineUser> machineUserMap) {
        this.groupMap = requireNonNull(groupMap);
        this.adminUserMap = requireNonNull(adminUserMap);
        this.adminUserRightsMap = requireNonNull(adminUserRightsMap);
        this.adminMachineUserMap = adminMachineUserMap;
        this.userMap = requireNonNull(userMap);
//        this.userRightsMap = requireNonNull(userRightsMap);
        this.machineUserMap = requireNonNull(machineUserMap);
//        this.machineUserRightsMap = requireNonNull(machineUserRightsMap);
    }

    public UsersState getUsersState(String environmentCrn) {
        UsersState.Builder builder = new UsersState.Builder();

        Map<String, com.sequenceiq.freeipa.api.v1.freeipa.user.model.Group> crnToGroup = new HashMap<>(groupMap.size());

        // TODO: No need to sync all the groups - remove below code
        groupMap.entrySet()
                .forEach(e -> {
                    com.sequenceiq.freeipa.api.v1.freeipa.user.model.Group group = umsGroupToGroup(e.getValue());
                    crnToGroup.put(e.getKey(), group);
                    builder.addGroup(group);
                });

        // TODO filter users by environment rights - Note: Now UmsState is for the envCrn.

        // Regular Users
        userMap.entrySet()
                .forEach(e -> {
                    com.sequenceiq.freeipa.api.v1.freeipa.user.model.User user = umsUserToUser(e.getValue());
                    builder.addUser(user);
//                    userRightsMap.get(e.getKey()).getGroupCrnList()
//                            .forEach(crn -> {
//                                builder.addMemberToGroup(crnToGroup.get(crn).getName(), user.getName());
//                            });
                });


        // env Admin Users
        adminUserMap.entrySet()
            .forEach(adminUser -> {
                // Admin users are also regular users but must be added to specific group
                com.sequenceiq.freeipa.api.v1.freeipa.user.model.User user = umsUserToUser(adminUser.getValue());
                builder.addUser(user);
                // TODO remove `admins` membership once the group mapping is figured out (CB-2003, DISTX-95)
                // TODO: change admins group to cpd_env_admin_<#env_name>, need to parse env crn and pass that group value.
                builder.addMemberToGroup("admins", user.getName());
            });

        // TODO filter machine users by environment rights
        machineUserMap.entrySet()
                .forEach(e -> {
                    com.sequenceiq.freeipa.api.v1.freeipa.user.model.User user = umsMachineUserToUser(e.getValue());
                    builder.addUser(user);
//                    machineUserRightsMap.get(e.getKey()).getGroupCrnList()
//                            .forEach(crn -> builder.addMemberToGroup(crnToGroup.get(crn).getName(), user.getName()));
                });

        // Machine Admin Users
        adminMachineUserMap.entrySet()
                .forEach(e -> {
                    com.sequenceiq.freeipa.api.v1.freeipa.user.model.User user = umsMachineUserToUser(e.getValue());
                    builder.addUser(user);
                    // TODO remove `admins` membership once the group mapping is figured out (CB-2003, DISTX-95)
                    // TODO: change admins group to cpd_env_admin_<#env_name>, need to parse env crn and pass that group value.
                    builder.addMemberToGroup("admins", user.getName());
//                    machineUserRightsMap.get(e.getKey()).getGroupCrnList()
//                            .forEach(crn -> builder.addMemberToGroup(crnToGroup.get(crn).getName(), user.getName()));
                });

        return builder.build();
    }

    public Set<String> getUsernamesFromCrns(Set<String> userCrns) {
        return userCrns.stream()
                .map(crn -> getWorkloadUsername(userMap.get(crn)))
                .collect(Collectors.toSet());
    }

    private com.sequenceiq.freeipa.api.v1.freeipa.user.model.User umsUserToUser(User umsUser) {
        com.sequenceiq.freeipa.api.v1.freeipa.user.model.User user = new com.sequenceiq.freeipa.api.v1.freeipa.user.model.User();
        user.setName(getWorkloadUsername(umsUser));
        user.setFirstName(getOrDefault(umsUser.getFirstName(), "None"));
        user.setLastName(getOrDefault(umsUser.getLastName(), "None"));
        return user;
    }

    private String getOrDefault(String value, String other) {
        return (value == null || value.isBlank()) ? other : value;
    }

    private String getWorkloadUsername(User umsUser) {
        return umsUser.getWorkloadUsername();
    }

    private com.sequenceiq.freeipa.api.v1.freeipa.user.model.User umsMachineUserToUser(MachineUser umsMachineUser) {
        com.sequenceiq.freeipa.api.v1.freeipa.user.model.User user = new com.sequenceiq.freeipa.api.v1.freeipa.user.model.User();
        user.setName(umsMachineUser.getWorkloadUsername());
        // TODO what should the appropriate first and last name be for machine users?
        user.setFirstName("Machine");
        user.setLastName("User");
        return user;
    }

    private com.sequenceiq.freeipa.api.v1.freeipa.user.model.Group umsGroupToGroup(Group umsGroup) {
        com.sequenceiq.freeipa.api.v1.freeipa.user.model.Group group = new com.sequenceiq.freeipa.api.v1.freeipa.user.model.Group();
        group.setName(umsGroup.getGroupName());
        return group;
    }

    public static class Builder {
        private Map<String, Group> groupMap = new HashMap<>();

        private Map<String, User> userMap = new HashMap<>();

//        private Map<String, GetRightsResponse> userRightsMap = new HashMap<>();

        private Map<String, User> adminUserMap = new HashMap<>();

//        private Map<String, GetRightsResponse> adminUserRightsMap = new HashMap<>();

        private Map<String, MachineUser> adminMachineUserMap = new HashMap<>();

        private Map<String, MachineUser> machineUserMap = new HashMap<>();

//        private Map<String, GetRightsResponse> machineUserRightsMap = new HashMap<>();

        public void addGroup(Group group) {
            groupMap.put(group.getCrn(), group);
        }

        public void addUser(User user, GetRightsResponse rights) {
            String userCrn = user.getCrn();
            userMap.put(userCrn, user);
//            userRightsMap.put(userCrn, rights);
        }

        public void addAdminUser(User user) {
            String userCrn = user.getCrn();
            adminUserMap.put(userCrn, user);
            //adminUserRightsMap.put(userCrn, rights);
        }

        public void addAdminMachineUser(MachineUser machineAdminuser) {
            // Machine User can be a power user also.
            String userCrn = machineAdminuser.getCrn();
            adminMachineUserMap.put(userCrn, machineAdminuser);
        }

        public void addMachineUser(MachineUser machineUser, GetRightsResponse rights) {
            String machineUserCrn = machineUser.getCrn();
            machineUserMap.put(machineUserCrn, machineUser);
//            machineUserRightsMap.put(machineUserCrn, rights);
        }

        public UmsState build() {
            return new UmsState(groupMap, adminUserMap, adminMachineUserMap, userMap, machineUserMap);
        }
    }
}
