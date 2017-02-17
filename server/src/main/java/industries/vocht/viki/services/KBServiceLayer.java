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

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import industries.vocht.viki.ApplicationException;
import industries.vocht.viki.IDao;
import industries.vocht.viki.aiml.AimlManager;
import industries.vocht.viki.hazelcast_messages.HMsgLoadEntities;
import industries.vocht.viki.hazelcast_messages.IHazelcastMessageProcessor;
import industries.vocht.viki.jersey.JsonMessage;
import industries.vocht.viki.model.knowledge_base.KBEntry;
import industries.vocht.viki.model.knowledge_base.KBEntryList;
import industries.vocht.viki.model.user.User;
import industries.vocht.viki.parser.NLParser;
import io.swagger.annotations.Api;
import org.glassfish.jersey.media.multipart.FormDataParam;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.*;
import java.util.*;

/**
 * Created by peter on 3/01/17.
 *
 * CRUD for address book entries
 *
 */
@Component
@Path("/viki/kb")
@Api(tags = "/viki/kb")
public class KBServiceLayer extends ServiceLayerCommon {

    private final Logger logger = LoggerFactory.getLogger(KBServiceLayer.class);

    // message to the user if this service layer isn't active
    private static final String sl_inactive_message = "knowledge-base service layer not active on this node";

    @Autowired
    private IDao dao;

    @Autowired
    private KBService kbService;

    @Autowired
    private IHazelcastMessageProcessor messageProcessor;

    @Autowired
    private NLParser parser;

    @Autowired
    private UserService userService;

    @Value("${sl.kb.activate:true}")
    private boolean slKBActive;

    /**
     * location for uploading files to - spring injected
     */
    @Value("${document.upload.directory:/tmp}")
    private String serverUploadLocation;

    /**
     * max size of a file allowed in MB (or infinite if <= 0) - spring injected
     */
    @Value("${document.max.upload.size.in.mb:100}")
    private int maxUploadSizeInMB;

    // manage and update the ai/ml system
    @Autowired
    private AimlManager aimlManager;



    public KBServiceLayer() {
    }


    /**
     * create/update a knowledge base entry
     * @param request the request object
     * @param sessionID the session of the active user
     * @param entry the knowledge-base entity to create or update
     * @return the update or created entity with id set if applicable
     */
    @PUT
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @Path("entry/{sessionID}")
    public Response save(@Context HttpServletRequest request,
                           @PathParam("sessionID") String sessionID,
                           KBEntry entry) {
        if (!slKBActive) {
            return Response.status(404).entity(new JsonMessage(sl_inactive_message)).build();
        }
        try {
            if ( entry != null ) {
                User user = checkSession( userService, sessionID, request );
                // new entry?  set origin to user's id if it doesn't have one
                if ( entry.getOrigin() == null ) {
                    entry.setOrigin(user.getEmail());
                }
                // we don't have a session - we need the minimum details for a create
                if ( entry.getType() == null || entry.getType().trim().length() == 0 || entry.getJson_data() == null ) {
                    logger.debug("POST: knowledge-base entry: invalid parameters");
                    return Response.status(500).entity(new JsonMessage("parameter(s) missing")).build();
                }
                entry.setOrganisation_id(user.getOrganisation_id()); // set always

                // was this an existing entry (ie. an update?) (but not a schema item)
                if (entry.getId() != null) {
                    kbService.unindex_entity(entry);
                }
                dao.getKBDao().saveKBEntry(entry);
                kbService.index_entity(entry, 0);

                // add any AI/ML for this entity to the AI/ML system
                aimlManager.updateAiml(entry);

                return Response.status(200).entity(entry).build();
            } else {
                logger.debug("POST: knowledge-base entry: invalid parameters");
                return Response.status(500).entity(new JsonMessage("parameter(s) missing")).build();
            }
        } catch (ApplicationException ex) {
            logger.debug("POST: knowledge-base entry: " + ex.getMessage());
            return Response.status(500).entity(new JsonMessage(ex.getMessage())).build();
        } catch ( Exception ex ) {
            logger.error("POST: knowledge-base entry: ",ex);
            return Response.status(500).entity(new JsonMessage(ex.getMessage())).build();
        }
    }


    /**
     * delete a knowledge-base entry
     * @param request the request object
     * @param sessionID the session of the active user
     * @param idStr the id of the knowledge-base entity to delete
     * @return returns 200 on success
     */
    @DELETE
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @Path("entry/{sessionID}/{type}/{id}")
    public Response delete( @Context HttpServletRequest request,
                            @PathParam("sessionID") String sessionID,
                            @PathParam("type") String type,
                            @PathParam("id") String idStr) {
        if (!slKBActive) {
            return Response.status(404).entity(new JsonMessage(sl_inactive_message)).build();
        }
        try {
            if ( idStr != null && sessionID != null && type != null) {
                User user = checkSession( userService, sessionID, request );
                UUID id = UUID.fromString(idStr);
                dao.getKBDao().deleteKBEntry(user.getOrganisation_id(), type, id);
                return Response.status(200).entity(new JsonMessage("ok", null)).build();
            } else {
                logger.debug("DELETE: knowledge-base entry: invalid parameters");
                return Response.status(500).entity(new JsonMessage("parameter(s) missing")).build();
            }
        } catch (ApplicationException ex) {
            logger.debug("DELETE: knowledge-base entry: " + ex.getMessage());
            return Response.status(500).entity(new JsonMessage(ex.getMessage())).build();
        } catch ( Exception ex ) {
            logger.error("DELETE: knowledge-base entry: ",ex);
            return Response.status(500).entity(new JsonMessage(ex.getMessage())).build();
        }
    }


    /**
     * get a of knowledge-base entry by uid
     * @param request the request object
     * @param sessionIDStr the session of the active user
     * @param idStr the guid id of the entity
     * @return a knowledge-base entry or 404 if not found
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("entry/{sessionID}/{type}/{id}")
    public Response entry( @Context HttpServletRequest request,
                           @PathParam("sessionID") String sessionIDStr,
                           @PathParam("type") String type,
                           @PathParam("id") String idStr ) {
        if ( !slKBActive) {
            return Response.status(404).entity(new JsonMessage(sl_inactive_message)).build();
        }
        try {
            if ( sessionIDStr != null && idStr != null && type != null ) {
                logger.debug("knowledge-base entry: " + sessionIDStr);
                User user = checkSession( userService, sessionIDStr, request );
                UUID id = UUID.fromString(idStr);
                KBEntry entry = dao.getKBDao().getKBEntry(user.getOrganisation_id(), type, id);
                if ( entry != null ) {
                    return Response.status(200).entity(entry).build();
                } else {
                    return Response.status(404).entity(new JsonMessage("entity not found")).build();
                }
            } else {
                logger.debug("knowledge-base entry: invalid parameters");
                return Response.status(500).entity(new JsonMessage("parameter(s) missing")).build();
            }
        } catch (ApplicationException ex){
            logger.debug("knowledge-base entry " + ex.getMessage());
            return Response.status(500).entity(new JsonMessage(ex.getMessage())).build();
        } catch ( Exception ex ) {
            logger.error("knowledge-base entry", ex);
            return Response.status(500).entity(new JsonMessage(ex.getMessage())).build();
        }
    }


    /**
     * create a new entity list
     * @param request the request object
     * @param sessionID the session of the active user
     * @param entityList the set/list of entities to create
     * @return 200 ok on success
     */
    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @Path("entry-list/{sessionID}")
    public Response createList(@Context HttpServletRequest request,
                               @PathParam("sessionID") String sessionID,
                               KBEntryList entityList ) {
        if ( !slKBActive ) {
            return Response.status(404).entity(new JsonMessage(sl_inactive_message)).build();
        }
        try {

            if ( entityList != null && entityList.getList() != null ) {

                for ( KBEntry entity : entityList.getList() ) {

                    // we don't have a session - we need the minimum details for a create
                    if (entity.getOrigin() == null || entity.getJson_data() == null || entity.getType() == null) {
                        logger.debug("POST: entry-list: invalid parameters");
                        return Response.status(500).entity(new JsonMessage("parameter(s) missing")).build();
                    }
                    User user = checkSession(userService, sessionID, request);
                    entity.setOrganisation_id(user.getOrganisation_id());
                    dao.getKBDao().saveKBEntry(entity);
                    kbService.index_entity(entity, 0);
                }

                // tell all lexicons to load these entities
                messageProcessor.publish(new HMsgLoadEntities());

                return Response.status(200).entity(new JsonMessage("ok",null)).build();

            } else {
                logger.debug("POST: entity-list: invalid parameters");
                return Response.status(500).entity(new JsonMessage("parameter(s) missing")).build();
            }
        } catch (ApplicationException ex) {
            logger.debug("POST: entity-list: " + ex.getMessage());
            return Response.status(500).entity(new JsonMessage(ex.getMessage())).build();
        } catch ( Exception ex ) {
            logger.error("POST: entity-list: ",ex);
            return Response.status(500).entity(new JsonMessage(ex.getMessage())).build();
        }
    }


    /**
     * list a set of knowledge-base entries paginated
     * @param request the request object
     * @param sessionIDStr the session of the active user
     * @param type the type of the entry to search for
     * @param prev the previous uuid string (or "null" for first page)
     * @param page_size number of items per page
     * @param json_field the json field to search on (ignored if this is "null" or query_str is "null")
     * @param query_str the query to execute (ignored if this is "null" or json_field is "null")
     * @return a list of knowledge-base entries or empty list if not found
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("list-entities/{sessionID}/{type}/{prev}/{page_size}/{json_field}/{query_str}")
    public Response listEntities( @Context HttpServletRequest request,
                                  @PathParam("sessionID") String sessionIDStr,
                                  @PathParam("type") String type,
                                  @PathParam("prev") String prev,
                                  @PathParam("page_size") int page_size,
                                  @PathParam("json_field") String json_field,
                                  @PathParam("query_str") String query_str ) {
        if ( !slKBActive) {
            return Response.status(404).entity(new JsonMessage(sl_inactive_message)).build();
        }
        try {
            if ( sessionIDStr != null && type != null && prev != null ) {
                logger.debug("knowledge-base list-entities: " + sessionIDStr);
                User user = checkSession( userService, sessionIDStr, request );
                UUID prev_uuid = null;
                if ( !"null".equals(prev)) {
                    prev_uuid = UUID.fromString(prev);
                }
                if ("null".equals(json_field) || "null".equals(query_str)) {
                    List<KBEntry> entryList = dao.getKBDao().getEntityList(user.getOrganisation_id(), type, prev_uuid, page_size);
                    return Response.status(200).entity(new KBEntryList(entryList)).build();
                } else {
                    List<KBEntry> entryList = kbService.findPaginated(user.getOrganisation_id(), json_field, type, query_str, prev_uuid, page_size);
                    return Response.status(200).entity(new KBEntryList(entryList)).build();
                }
            } else {
                logger.debug("knowledge-base list-entities: invalid parameters");
                return Response.status(500).entity(new JsonMessage("parameter(s) missing")).build();
            }
        } catch (ApplicationException ex){
            logger.debug("knowledge-base list-entities" + ex.getMessage());
            return Response.status(500).entity(new JsonMessage(ex.getMessage())).build();
        } catch ( Exception ex ) {
            logger.error("knowledge-base list-entities", ex);
            return Response.status(500).entity(new JsonMessage(ex.getMessage())).build();
        }
    }



    /**
     * upload a json set to the system for a given type
     * to test:
     * curl -v -X POST --form file=@dol.png --form payload=junk https://localhost:8443/1.0/pg/seeker/upload -k
     *
     * @param fileInputStream          the stream to the object being uploaded
     * @param sessionID the session identifier / security for this user
     * @param type the type of the object to upload
     * @return a proper 200 response on success
     */
    @POST
    @Path("upload-for-type/{sessionID}/{type}/{filename}")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON)
    public Response upload(@Context HttpServletRequest request,
                           @FormDataParam("file") final InputStream fileInputStream,
                           @PathParam("sessionID") String sessionID,
                           @PathParam("type") String type,
                           @PathParam("filename") String filename) {

        if ( !slKBActive ) {
            return Response.status(404).entity(new JsonMessage(sl_inactive_message)).build();
        }
        if ( sessionID == null || type == null || fileInputStream == null ) {
            return Response.status(500).entity(new JsonMessage("invalid parameters")).build();
        }
        try {
            // check session
            User user = userService.getUser(UUID.fromString(sessionID), request.getRemoteAddr());
            if (user == null) {
                logger.debug("kb/upload-for-type invalid session (" + sessionID + ")");
                return Response.status(500).entity(new JsonMessage("invalid session")).build();
            } else {

                String jsonFilename = serverUploadLocation + "/" + UUID.randomUUID().toString() + ".json";
                try {
                    if (!filename.endsWith(".jsn") && !filename.endsWith(".json")) {
                        throw new IOException("Error: filename must end in \".jsn\" or \".json\".");
                    }

                    ObjectMapper mapper = new ObjectMapper();

                    // create a local copy of this file
                    saveFile(fileInputStream, jsonFilename);

                    // get the schema for this type
                    KBEntry protoType = getSchemaByName(user.getOrganisation_id(), type);
                    if ( protoType == null ) {
                        throw new IOException("KB entry type \"" + type + "\" not found");
                    }
                    Map<String, Object> protoMap = mapper.readValue(protoType.getJson_data(), new TypeReference<Map<String, Object>>() {});
                    if (!protoMap.containsKey("field_list") || !(protoMap.get("field_list") instanceof List)) {
                        throw new IOException("KB entry type \"" + type + "\" invalid field_list");
                    }
                    List fieldList = (List)protoMap.get("field_list");
                    List<String> actualFieldList = new ArrayList<>();
                    // get the field descriptors out of the map
                    for ( Object item : fieldList ) {
                        if (item instanceof Map) {
                            Map itemMap = (Map)item;
                            if (itemMap.containsKey("name") && itemMap.containsKey("indexed") ) {
                                if ((itemMap.get("indexed") instanceof Boolean) && (itemMap.get("name") instanceof String)) {
                                    actualFieldList.add((String)itemMap.get("name"));
                                }
                            }
                        }
                    }
                    HashSet<String> fieldLookup = new HashSet<>(actualFieldList);

                    // read this file line by line
                    try (BufferedReader br = new BufferedReader(new FileReader(jsonFilename))) {
                        String line;
                        int line_counter = 0;
                        while ((line = br.readLine()) != null) {
                            line_counter += 1;
                            line = line.trim();
                            if (line.length() > 0) {
                                Map<String, Object> map = mapper.readValue(line, new TypeReference<Map<String, Object>>() {});
                                // check keys are good
                                for (String key:map.keySet()) {
                                    if (!key.equals("id") && !fieldLookup.contains(key)) {
                                        throw new IOException("invalid field in import file " + filename + ", line " + line_counter +
                                                ", unknown field for type \"" + type + "\", \"" + key + "\"");
                                    }
                                }

                                // get an id for this object if present
                                UUID id = null;
                                if (map.containsKey("id")) {
                                    id = UUID.fromString((String)map.get("id"));
                                }

                                // store this item
                                KBEntry entry = new KBEntry(user.getOrganisation_id(), id, type, "uploaded by " + user.getEmail(), line);
                                dao.getKBDao().saveKBEntry(entry);

                                // index this entity
                                kbService.index_entity(entry, 0);

                            } // if line has length
                        } // for each line in file
                    } // try open

                    // all is ok - return
                    logger.debug("kb/upload complete " + sessionID + ", " + filename);
                    return Response.status(200).entity(new JsonMessage("document successfully uploaded",null)).build();

                } catch (IOException ex) {
                    return Response.status(500).entity(new JsonMessage("IO Exception:" + ex.getMessage())).build();
                } finally {
                    // remove file at end
                    if (new File(jsonFilename).exists()) {
                        new File(jsonFilename).delete();
                    }
                }
            }
        } catch (Exception ex) {
            logger.error("kb/upload-for-type", ex);
            return Response.status(500).entity(new JsonMessage(ex.getMessage())).build();
        }
    }

    /**
     * find a schema by name
     * @param organisation_id the organisation
     * @param type the type / name of the schema item
     * @return null if not found, otherwise the item
     */
    private KBEntry getSchemaByName(UUID organisation_id, String type) throws IOException {
        UUID prev = null;
        KBEntry schema = null;
        ObjectMapper mapper = new ObjectMapper();
        while (schema == null) {
            List<KBEntry> entryList = dao.getKBDao().getEntityList(organisation_id, "schema", prev, 10);
            if ( entryList != null && entryList.size() > 0 ) {
                for (KBEntry entry : entryList) {
                    Map<String, Object> protoMap = mapper.readValue(entry.getJson_data(), new TypeReference<Map<String, Object>>() {});
                    if (!protoMap.containsKey("field_list") ||
                            !(protoMap.get("field_list") instanceof List) || !protoMap.containsKey("name")) {
                        throw new IOException("KB entry type \"" + type + "\" invalid name/field_list");
                    }
                    String name = (String)protoMap.get("name");
                    if (type.equals(name)) {
                        schema = entry;
                        break;
                    }
                }
                prev = entryList.get(entryList.size()-1).getId();
            } else {
                break;
            }
        }
        return schema;
    }

    /**
     * save uploaded file to a defined location on the server
     *
     * @param uploadedInputStream the stream to the object being uploaded
     * @param serverLocation      the location on the server to use
     */
    private void saveFile(InputStream uploadedInputStream, String serverLocation) throws IOException {
        try {
            OutputStream outputStream;
            int read;
            byte[] bytes = new byte[1024];
            long totalSize = 0;
            long maxSize = (long) maxUploadSizeInMB * 1024_000L;
            outputStream = new FileOutputStream(new File(serverLocation));
            while ((read = uploadedInputStream.read(bytes)) != -1) {
                outputStream.write(bytes, 0, read);
                totalSize = totalSize + read;
                // only check if positive
                if (maxSize > 0 && totalSize > maxSize)
                    throw new IOException("file exceeds maximum allowed size of " + maxUploadSizeInMB + "MB");
            }
            outputStream.flush();
            outputStream.close();

            logger.info("saved " + totalSize + " bytes to " + serverLocation);

        } catch (IOException ex) {
            logger.error("upload.saveFile", ex);
        }
    }


}



