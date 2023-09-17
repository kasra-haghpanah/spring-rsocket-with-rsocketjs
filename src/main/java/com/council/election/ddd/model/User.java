package com.council.election.ddd.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.bson.types.ObjectId;
import org.hibernate.validator.constraints.Length;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.mongodb.core.index.IndexDirection;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import javax.validation.constraints.NotNull;
import java.util.*;
import java.util.stream.Collectors;

@Document(collection = "user")
//@CompoundIndex(def = "{'username': 1 }", name = "username_index_asc", unique = true)
public class User implements UserDetails {

    @Id
    @Field(name = "_id")
    private ObjectId id;

    @Field(name = "parent_id")
    private ObjectId parentId;

    @Field(name = "path")
    private String path;

    @NotNull(message = "firstName may not be empty")
    @Length(max = 50, min = 1, message = "firstName length must be > 1 && <= 50")
    @Field(name = "first_name")
    private String firstName;

    @NotNull(message = "lastName may not be empty")
    @Length(max = 100, min = 1, message = "firstName length must be > 1 && <= 100")
    @Field(name = "last_name")
    private String lastName;

    //@Email
    @NotNull(message = "phone may not be empty")
    @Field(name = "username")//, unique = true
    @Indexed(unique = true, direction = IndexDirection.ASCENDING, name = "username_index_asc")
    private String username;

    //@Size(min = 3, max = 60)
    //@NotNull(message = "password may not be empty")
    @Field(name = "password")
    private String password;

    @Field(name = "activationCode")
    private String activationCode;

    //@NotEmpty(message = "roles may not be empty")
    @NotNull(message = "roles may not be empty")
    @Field(name = "roles")
    private Set<String> roles;

    @Field(name = "metadata")
    private org.bson.Document metadata;

    @Transient
    private final ObjectMapper objectMapper = new ObjectMapper();


    @Override
    public boolean isAccountNonExpired() {
        return false;
    }

    @Override
    public boolean isAccountNonLocked() {
        return false;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return false;
    }


    @Override
    public boolean isEnabled() {
        return (this.activationCode == null || this.activationCode.equals("")) ? true : false;
    }


    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {

        Set<String> roles = getRoles();
        if (roles == null) {
            return null;
        }
        return roles.stream().map(role -> new SimpleGrantedAuthority(role)).collect(Collectors.toSet());
    }

    public ObjectId getId() {
        return id;
    }

    public String getIdHexString() {
        return id.toHexString();
    }

    public void setId(ObjectId id) {
        this.id = id;
    }

    public void setId(String id) {
        if (id == null) {
            return;
        }
        this.id = new ObjectId(id);
    }

    public ObjectId getParentId() {
        return parentId;
    }

    public String getParentIdHexString() {
        return parentId.toHexString();
    }

    public void setParentId(ObjectId parentId) {
        if (parentId == null) {
            return;
        }
        this.parentId = parentId;
    }

    public void setParentId(String parentId) {
        if (parentId != null && !parentId.equals("")) {
            this.parentId = new ObjectId(parentId);
        }
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
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

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    @Override
    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        if (password != null && !password.equals("")) {
            this.password = new BCryptPasswordEncoder().encode(password); //PasswordEncoderFactories.createDelegatingPasswordEncoder().encode(password);
        }
    }

    public Set<String> getRoles() {
        return roles;
    }

    public void setRoles(String... roles) {
        if (roles != null) {
            if (this.roles == null) {
                this.roles = new HashSet<String>();
            }
            for (String role : roles) {
                this.roles.add(role);
            }
        }
    }

    public void setRoles(Set<String> roles) {
        this.roles = roles;
    }

    public void setRoles(List<String> roles) {
        if (roles != null) {
            if (this.roles == null) {
                this.roles = new HashSet<String>();
            }
            for (String role : roles) {
                this.roles.add(role);
            }
        }
    }

    public String getActivationCode() {
        return activationCode;
    }

    public void setActivationCode(String activationCode) {
        this.activationCode = activationCode;
    }

    public org.bson.Document getMetadata() {
        return metadata;
    }

    public void setMetadata(org.bson.Document metadata) {
        this.metadata = metadata;
    }

    public static User toUser(org.bson.Document map) {
        User user = new User();
        if (map.get("id") != null) {
            user.setId((String) map.get("id"));
        }
        if (map.get("parent_id") != null) {
            user.setParentId((String) map.get("parent_id"));
        }
        if (map.get("first_name") != null) {
            user.setFirstName((String) map.get("first_name"));
        }
        if (map.get("last_name") != null) {
            user.setLastName((String) map.get("last_name"));
        }
        if (map.get("username") != null) {
            user.setUsername((String) map.get("username"));
        }
        if (map.get("activationCode") != null) {
            user.setActivationCode((String) map.get("activationCode"));
        }
        if (map.get("password") != null) {
            user.setPassword((String) map.get("password"));
        }
        if (map.get("path") != null) {
            user.setPath((String) map.get("path"));
        }
        if (map.get("roles") != null) {
            user.setRoles((List<String>) map.get("roles"));
        }
        if (map.get("metadata") != null) {
            user.setMetadata((org.bson.Document) map.get("metadata"));
        }
        return user;
    }
}
