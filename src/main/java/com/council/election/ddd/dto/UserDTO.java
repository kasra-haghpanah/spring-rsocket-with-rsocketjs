package com.council.election.ddd.dto;


import com.council.election.ddd.model.User;
import org.bson.Document;

import java.util.HashMap;
import java.util.Map;

public class UserDTO {

    private String id;
    private String parentId;
    private String username;
    private String firstName;
    private String lastName;
    private String fullName;
    private int childsCount;
    private int activeChildsCount;
    private boolean active;
    private boolean leafNode = true;
    private String code = "";
    private Map<String, Object> metadata;

    public UserDTO() {
    }

    public UserDTO(User user) {
        this.setId(user.getId().toHexString());
        if (user.getParentId() != null) {
            this.setParentId(user.getParentId().toHexString());
        }
        this.setUsername(user.getUsername());
        this.setFirstName(user.getFirstName());
        this.setLastName(user.getLastName());
        this.setFullName(user.getFirstName() + " " + user.getLastName());
        this.setCode(user.getActivationCode());
        this.setActive(user.getActivationCode() == null ? true : false);
    }

    public static UserDTO getUserDTOFromDocument(Document map) {
        if (map == null) {
            return null;
        }
        UserDTO user = new UserDTO();
        if (map.get("id") != null) {
            user.setId(map.getString("id"));
        }
        if (user.getId() == null && map.get("_id") != null) {
            user.setId((map.getObjectId("_id")).toHexString());
        }
        if (map.get("parent_id") != null) {
            if (map.get("parent_id") instanceof String) {
                user.setParentId(map.getString("parent_id"));
            } else {
                user.setParentId(map.getObjectId("parent_id").toHexString());
            }

        }
        user.setFirstName(map.getString("first_name"));
        user.setLastName(map.getString("last_name"));
        user.setFullName(user.getFirstName() + " " + user.getLastName());
        user.setUsername(map.getString("username"));
        if (map.get("childsCount") != null) {
            user.setChildsCount(map.getInteger("childsCount"));
        }

        if (map.get("activeChildsCount") != null) {
            user.setActiveChildsCount(map.getInteger("activeChildsCount"));
        }
        if (map.get("leafNode") != null) {
            user.setLeafNode(map.getBoolean("leafNode"));
        }
        if (map.get("active") != null) {
            user.setActive(map.getBoolean("active"));
        }

        if (map.get("code") != null) {
            user.setCode(map.getString("code"));
        }

        if (map.get("metadata") != null) {
            Document metadata = map.get("metadata", Document.class);
            if (metadata != null && !metadata.isEmpty()) {
                Map<String, Object> objectMap = new HashMap<String, Object>();
                for (String key : metadata.keySet()) {
                    objectMap.put(key, metadata.get(key));
                }
                user.setMetadata(objectMap);
            }
        }

        return user;
    }

    public User toEntity() {
        User user = new User();
        user.setId(this.id);
        user.setParentId(this.parentId);
        user.setUsername(this.username);
        user.setFirstName(this.firstName);
        user.setLastName(this.lastName);
        if (this.metadata != null && this.metadata.keySet().size() > 0) {
            Document document = new Document();
            for (String key : this.metadata.keySet()) {
                document.append(key, this.metadata.get(key));
            }
            user.setMetadata(document);
        }
        return user;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getParentId() {
        return parentId;
    }

    public void setParentId(String parentId) {
        this.parentId = parentId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public int getChildsCount() {
        return childsCount;
    }

    public void setChildsCount(int childsCount) {
        this.childsCount = childsCount;
    }

    public int getActiveChildsCount() {
        return activeChildsCount;
    }

    public void setActiveChildsCount(int activeChildsCount) {
        this.activeChildsCount = activeChildsCount;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public boolean isLeafNode() {
        return leafNode;
    }

    public void setLeafNode(boolean leafNode) {
        this.leafNode = leafNode;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }
}
