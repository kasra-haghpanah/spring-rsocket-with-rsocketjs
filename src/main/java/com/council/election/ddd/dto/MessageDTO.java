package com.council.election.ddd.dto;

import org.bson.types.ObjectId;

/**
 * Created by kasra.haghpanah on 05/12/2019.
 */
public class MessageDTO {

    private String id;
    private String title;
    private String text;
    private String phone;
    private String firstName;
    private String lastName;
    private String fileId;
    private String contentType;
    private String filename;
    private int size;
    private double duration;

    public MessageDTO() {
    }

    public String getId() {
        return id;
    }

    public MessageDTO setId(String id) {
        this.id = id;
        return this;
    }

    public String getTitle() {
        return title;
    }

    public MessageDTO setTitle(String title) {
        this.title = title;
        return this;
    }

    public String getText() {
        return text;
    }

    public MessageDTO setText(String text) {
        this.text = text;
        return this;
    }

    public String getPhone() {
        return phone;
    }

    public MessageDTO setPhone(String phone) {
        this.phone = phone;
        return this;
    }

    public String getFirstName() {
        return firstName;
    }

    public MessageDTO setFirstName(String firstName) {
        this.firstName = firstName;
        return this;
    }

    public String getLastName() {
        return lastName;
    }

    public MessageDTO setLastName(String lastName) {
        this.lastName = lastName;
        return this;
    }

    public String getFileId() {
        return fileId;
    }

    public MessageDTO setFileId(String fileId) {
        this.fileId = fileId;
        return this;
    }

    public String getContentType() {
        return contentType;
    }

    public MessageDTO setContentType(String contentType) {
        this.contentType = contentType;
        return this;
    }

    public String getFilename() {
        return filename;
    }

    public MessageDTO setFilename(String filename) {
        this.filename = filename;
        return this;
    }

    public int getSize() {
        return size;
    }

    public MessageDTO setSize(int size) {
        this.size = size;
        return this;
    }

    public double getDuration() {
        return duration;
    }

    public MessageDTO setDuration(double duration) {
        this.duration = duration;
        return this;
    }
}
