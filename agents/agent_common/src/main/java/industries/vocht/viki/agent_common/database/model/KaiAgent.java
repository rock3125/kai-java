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

package industries.vocht.viki.agent_common.database.model;

/**
 * Created by peter on 16/06/16.
 *
 * kai agent type
 *
 */
public class KaiAgent {

    // uid of the agent
    private int id;
    // name of the agent
    private String name;

    // credentials for the remote thing this agent communicates with
    private String username;
    private String password;
    private String domain;
    private String server; // the thing to talk to
    private String path; // the path on that thing

    // time schedule (e.g. "mon 01:00-02:00,tue 01:00-02:00")
    private String schedule;
    // how many files per second max to get (throttle)
    private int files_per_second;
    // arbitrary remote token for storage if so required (e.g. Exchange resume tokens)
    private String remote_token;

    // KAI interface access
    private String kai_username;
    private String kai_password;
    // kai server end-point
    private String kai_login_server;
    private int kai_login_port;
    private String kai_document_server;
    private int kai_document_port;

    public KaiAgent() {
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getDomain() {
        return domain;
    }

    public void setDomain(String domain) {
        this.domain = domain;
    }

    public String getSchedule() {
        return schedule;
    }

    public void setSchedule(String schedule) {
        this.schedule = schedule;
    }

    public int getFiles_per_second() {
        return files_per_second;
    }

    public void setFiles_per_second(int files_per_second) {
        this.files_per_second = files_per_second;
    }

    public String getRemote_token() {
        return remote_token;
    }

    public void setRemote_token(String remote_token) {
        this.remote_token = remote_token;
    }

    public String getKai_username() {
        return kai_username;
    }

    public void setKai_username(String kai_username) {
        this.kai_username = kai_username;
    }

    public String getKai_password() {
        return kai_password;
    }

    public void setKai_password(String kai_password) {
        this.kai_password = kai_password;
    }

    public String getKai_login_server() {
        return kai_login_server;
    }

    public void setKai_login_server(String kai_login_server) {
        this.kai_login_server = kai_login_server;
    }

    public String getServer() {
        return server;
    }

    public void setServer(String server) {
        this.server = server;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public int getKai_login_port() {
        return kai_login_port;
    }

    public void setKai_login_port(int kai_login_port) {
        this.kai_login_port = kai_login_port;
    }

    public String getKai_document_server() {
        return kai_document_server;
    }

    public void setKai_document_server(String kai_document_server) {
        this.kai_document_server = kai_document_server;
    }

    public int getKai_document_port() {
        return kai_document_port;
    }

    public void setKai_document_port(int kai_document_port) {
        this.kai_document_port = kai_document_port;
    }
}

