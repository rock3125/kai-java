package industries.vocht.viki.client;

import industries.vocht.viki.jersey.JsonMessage;
import industries.vocht.viki.model.SpacyPacketList;
import industries.vocht.viki.model.SpacyTokenList;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.message.BasicHeader;
import org.apache.http.protocol.HTTP;

import java.io.IOException;

/**
 * Created by peter on 11/12/16.
 *
 * Spacy/WSD endpoint integration for a faster, better SRL / tagger amd nnet WSD
 *
 */
public class PythonClientInterface extends ClientInterfaceCommon {

    public PythonClientInterface(String host, int port ) {
        super(host, port);
    }


    public SpacyTokenList parse(String text) throws IOException {
        if ( text != null && text.length() > 0 ) {
            HttpPost post = new HttpPost(serverAddress + "/parse");

            // set the entity and its mime-type
            StringEntity se = new StringEntity(text);
            se.setContentType(new BasicHeader(HTTP.CONTENT_TYPE, "application/json"));
            post.setEntity(se);

            HttpResponse httpResponse = send(post);

            // Then
            if (httpResponse.getStatusLine().getStatusCode() == 200) { // check 200
                return retrieveResourceFromResponse(httpResponse, SpacyTokenList.class);
            } else {
                JsonMessage message = retrieveResourceFromResponse(httpResponse, JsonMessage.class);
                throw new IOException(message.toString());
            }
        }
        return null;
    }


    public SpacyPacketList parsePackage(String text) throws IOException {
        if ( text != null && text.length() > 0 ) {
            HttpPost post = new HttpPost(serverAddress + "/parse-package");

            // set the entity and its mime-type
            StringEntity se = new StringEntity(text);
            se.setContentType(new BasicHeader(HTTP.CONTENT_TYPE, "application/json"));
            post.setEntity(se);

            HttpResponse httpResponse = send(post);

            // Then
            if (httpResponse.getStatusLine().getStatusCode() == 200) { // check 200
                return retrieveResourceFromResponse(httpResponse, SpacyPacketList.class);
            } else {
                JsonMessage message = retrieveResourceFromResponse(httpResponse, JsonMessage.class);
                throw new IOException(message.toString());
            }
        }
        return null;
    }


}

