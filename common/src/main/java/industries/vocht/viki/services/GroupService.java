/*
 * Copyright (c) 2016 by Peter de Vocht
 *
 * All rights reserved. No part of this publication may be reproduced, distributed, or
 * transmitted in any form or by any means, including photocopying, recording, or other
 * electronic or mechanical methods, without the prior written permission of the publisher,
 * except in the case of brief quotations embodied in critical reviews and certain other
 * noncommercial uses permitted by copyright law.
 *
 */

package industries.vocht.viki.services;

import industries.vocht.viki.ApplicationException;
import industries.vocht.viki.IDao;
import industries.vocht.viki.model.group.Group;
import industries.vocht.viki.model.group.GroupList;
import industries.vocht.viki.model.user.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Created by peter on 4/03/16.
 *
 */
@Component
public class GroupService {

    private final static Logger logger = LoggerFactory.getLogger(GroupService.class);

    @Autowired
    private IDao dao; // dao access

    public GroupService() {
    }

    /**
     * get a group by name
     * @param sessionID the security session
     * @param name the name of the group
     * @param ipAddress the ip address for security purposes
     * @return the group
     * @throws ApplicationException
     */
    public Group getGroup(UUID sessionID, String name, String ipAddress) throws ApplicationException {
        if ( sessionID == null || ipAddress == null || name == null ) {
            throw new ApplicationException("getGroup: invalid parameter");
        }
        User sessionUser = dao.getUserDao().getUserForSession(sessionID, ipAddress);
        if ( sessionUser == null ) {
            throw new ApplicationException("getGroup: invalid session");
        }
        return dao.getGroupDao().read(sessionUser.getOrganisation_id(), name);
    }

    /**
     * create a new group
     * @param sessionID the id of a security session
     * @param group the group object to create
     * @param ipAddress the security ip address
     * @return the created group with its id set
     * @throws ApplicationException
     */
    public Group createGroup(UUID sessionID, Group group, String ipAddress ) throws ApplicationException {
        if ( sessionID == null || ipAddress == null || group == null || group.getName() == null ) {
            throw new ApplicationException("createGroup: invalid parameter");
        }
        User sessionUser = dao.getUserDao().getUserForSession(sessionID, ipAddress);
        if ( sessionUser == null ) {
            throw new ApplicationException("createGroup: invalid session");
        }
        // make sure the group doesn't exist
        Group existingGroup = dao.getGroupDao().read(sessionUser.getOrganisation_id(), group.getName());
        if ( existingGroup != null ) {
            throw new ApplicationException("createGroup: a group with that name already exists");
        }
        group.setOrganisation_id( sessionUser.getOrganisation_id() );
        Group group1 = dao.getGroupDao().create(sessionUser.getOrganisation_id(), group);

        return group1;
    }

    /**
     * update an existing group
     * @param sessionID the id of a security session
     * @param group the group object to update
     * @param ipAddress the security ip address
     * @throws ApplicationException
     */
    public void updateGroup(UUID sessionID, Group group, String ipAddress ) throws ApplicationException {
        if ( sessionID == null || ipAddress == null || group == null || group.getName() == null || group.getUser_list() == null || group.getUser_list().size() == 0 ) {
            throw new ApplicationException("updateGroup: invalid parameter");
        }
        User sessionUser = dao.getUserDao().getUserForSession(sessionID, ipAddress);
        if ( sessionUser == null ) {
            throw new ApplicationException("updateGroup: invalid session");
        }
        // make sure the group exists!
        Group existingGroup = dao.getGroupDao().read(sessionUser.getOrganisation_id(), group.getName());
        if ( existingGroup == null ) {
            throw new ApplicationException("updateGroup: a group with that name does not exist");
        }
        dao.getGroupDao().update(sessionUser.getOrganisation_id(), group);
    }

    /**
     * delete an existing group
     * @param sessionID the id of a security session
     * @param name the name of the group to remove
     * @param ipAddress the security ip address
     * @throws ApplicationException
     */
    public void deleteGroup(UUID sessionID, String name, String ipAddress ) throws ApplicationException {
        if ( sessionID == null || ipAddress == null || name == null ) {
            throw new ApplicationException("deleteGroup: invalid parameter");
        }
        User sessionUser = dao.getUserDao().getUserForSession(sessionID, ipAddress);
        if ( sessionUser == null ) {
            throw new ApplicationException("deleteGroup: invalid session");
        }
        // make sure the group exists
        Group existingGroup = dao.getGroupDao().read(sessionUser.getOrganisation_id(), name);
        if ( existingGroup == null ) {
            throw new ApplicationException("deleteGroup: a group with that name does not exist");
        }

        dao.getGroupDao().delete(sessionUser.getOrganisation_id(), name);
    }


    /**
     * add an existing user to an existing group
     * @param sessionID the id of a security session
     * @param groupName the name of the group to add a user to
     * @param userName the name of the user to add to the group
     * @param ipAddress the security ip address
     * @return the updated group object
     * @throws ApplicationException
     */
    public Group addUserToGroup(UUID sessionID, String groupName, String userName, String ipAddress ) throws ApplicationException {
        if ( sessionID == null || ipAddress == null || groupName == null || userName == null ) {
            throw new ApplicationException("addUserToGroup: invalid parameter");
        }
        User sessionUser = dao.getUserDao().getUserForSession(sessionID, ipAddress);
        if ( sessionUser == null ) {
            throw new ApplicationException("addUserToGroup: invalid session");
        }
        // make sure the group exists
        Group existingGroup = dao.getGroupDao().read(sessionUser.getOrganisation_id(), groupName);
        if ( existingGroup == null ) {
            throw new ApplicationException("addUserToGroup: a group with that name does not exist");
        }
        User existingUser = dao.getUserDao().read(userName);
        if ( existingUser == null ) {
            throw new ApplicationException("addUserToGroup: a user with that email does not exist");
        }
        List<String> userList = existingGroup.getUser_list();
        if ( userList == null ) {
            userList = new ArrayList<>();
            existingGroup.setUser_list(userList);
        }
        userList.add(existingUser.getEmail());
        dao.getGroupDao().update(sessionUser.getOrganisation_id(), existingGroup);
        return existingGroup;
    }

    /**
     * remove an existing user from an existing group
     * @param sessionID the id of a security session
     * @param groupName the name of the group to remove the user from
     * @param userName the name of the user to remove from the group
     * @param ipAddress the security ip address
     * @return the updated group object
     * @throws ApplicationException
     */
    public Group removeUserFromGroup(UUID sessionID, String groupName, String userName, String ipAddress ) throws ApplicationException {
        if ( sessionID == null || ipAddress == null || groupName == null || userName == null ) {
            throw new ApplicationException("removeUserFromGroup: invalid parameter");
        }
        User sessionUser = dao.getUserDao().getUserForSession(sessionID, ipAddress);
        if ( sessionUser == null ) {
            throw new ApplicationException("removeUserFromGroup: invalid session");
        }
        // make sure the group exists
        Group existingGroup = dao.getGroupDao().read(sessionUser.getOrganisation_id(), groupName);
        if ( existingGroup == null ) {
            throw new ApplicationException("removeUserFromGroup: a group with that name does not exist");
        }
        User existingUser = dao.getUserDao().read(userName);
        if ( existingUser == null ) {
            throw new ApplicationException("removeUserFromGroup: a user with that email does not exist");
        }
        List<String> userList = existingGroup.getUser_list();
        if ( userList != null ) {
            userList.remove(userName);
            dao.getGroupDao().update(sessionUser.getOrganisation_id(), existingGroup);
        }
        return existingGroup;
    }

    /**
     *
     * @param sessionID the id of a security session
     * @param page page index in num items per page
     * @param numItemsPerPage items per page
     * @param ipAddress the security ip address
     * @return group list object
     * @throws ApplicationException
     */
    public GroupList getPaginatedGroupList(UUID sessionID, int page, int numItemsPerPage,
                                           String ipAddress) throws ApplicationException {
        if ( sessionID == null || ipAddress == null ) {
            throw new ApplicationException("getPaginatedGroupList: invalid parameter");
        }
        User sessionUser = dao.getUserDao().getUserForSession(sessionID, ipAddress);
        if ( sessionUser == null ) {
            throw new ApplicationException("getPaginatedGroupList: invalid session");
        }
        List<Group> groupNameList = dao.getGroupDao().readAllGroups(sessionUser.getOrganisation_id());
        List<Group> groupList = new ArrayList<>();
        int nameListSize = 0;
        if ( groupNameList != null ) {
            nameListSize = groupNameList.size();
            int offset = page * numItemsPerPage;
            int endOffset = offset + numItemsPerPage;
            for (int i = offset; i < endOffset; i++) {
                if (i < groupNameList.size()) {
                    groupList.add(groupNameList.get(i));
                }
            }
        }
        GroupList list = new GroupList();
        list.setGroup_list(groupList);
        list.setOrganisation_id(sessionUser.getOrganisation_id());
        list.setPage(page);
        list.setItems_per_page(numItemsPerPage);
        list.setTotal_group_count(nameListSize);
        return list;
    }

    /**
     * convert a set of group names to an actual list of groups
     * @param list a list of group names
     * @param page page offset into the list
     * @param numItemsPerPage num of items per page
     * @return a list of groups (or an empty object)
     */
    public GroupList getListFromGroupNames(UUID organisation_id, List<String> list, int page, int numItemsPerPage) {
        if ( list != null && list.size() > 0 ) {

            GroupList groupList = new GroupList();
            groupList.setPage(page);
            groupList.setItems_per_page(numItemsPerPage);
            groupList.setTotal_group_count(list.size());

            int offset = page * numItemsPerPage;
            int endOffset = offset + numItemsPerPage;
            for (int i = offset; i < endOffset; i++) {
                if (i < list.size()) {
                    Group group = dao.getGroupDao().read(organisation_id, list.get(i));
                    if (group != null) {
                        groupList.getGroup_list().add(group);
                    }
                }
            }
            return groupList;
        } else {
            return new GroupList();
        }
    }


}

