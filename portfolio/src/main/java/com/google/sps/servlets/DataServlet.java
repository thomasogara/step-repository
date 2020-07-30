// Copyright 2019 Google LLC
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     https://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.google.sps.servlets;


import com.google.appengine.api.blobstore.BlobInfo;
import com.google.appengine.api.blobstore.BlobInfoFactory;
import com.google.appengine.api.blobstore.BlobKey;
import com.google.appengine.api.blobstore.BlobstoreService;
import com.google.appengine.api.blobstore.BlobstoreServiceFactory;
import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.api.datastore.PreparedQuery;
import com.google.appengine.api.datastore.Query.SortDirection;
import com.google.appengine.api.users.UserService;
import com.google.appengine.api.users.UserServiceFactory;
import com.google.gson.Gson;
import com.google.sps.data.Comment;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.net.MalformedURLException;
import java.util.Map;
import java.net.URL;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/*
 * The /comments route exposes a simple API
 * This API can be accessed by the GET and POST http methods only.
 * The API allows a client to either:
 *   - Get a programmable number of comments from the server
 *   - Post a single comment to the server and store it in the datastore
 *
 * GET:
 * The request body does not have any mandatory parameters.
 * The request body may optionally contain:
 *   - A single parameter, 'maxComments'
 * If the maxComments parameter is included, then maxComments is used to limit
 * the number of results returned by the request.
 * The response body will be encoded as json.
 * The response body will contain a single top-level array, whose
 * elements will all be Comment objects.
 * Comment objects have five members:
 *   id: the id of the comment in the server's datastore
 *   title: the comment title
 *   text: the comment text
 *   timestamp: the comment creation time
 *   imageBlobstoreKey: the key of the comment image in BlobStore
 *
 * POST:
 * The request body must contain:
 *   - A 'text' parameter
 * The request body may optionally contain:
 *   - A 'title' parameter
 *   - A 'imageURL' parameter
 * The response will be a redirect to the homepage of the site.
 */

/** Servlet that returns a programmable number of comments */
@WebServlet("/comments")
public class DataServlet extends HttpServlet {
  public static final String NO_IMAGE_UPLOAD = "";

  @Override
  public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
    long maxComments = Long.parseLong(request.getParameter("maxComments"));

    // a value of -1 indicates that ALL comments are wanted
    if (maxComments == -1) maxComments = Long.MAX_VALUE;
    
    Query query = new Query("Comment").addSort("timestamp", SortDirection.DESCENDING);

    DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
    PreparedQuery results = datastore.prepare(query);

    List<Comment> comments = new ArrayList<>();
    for ( Entity entity : results.asIterable() ) {
      if (comments.size() >= maxComments ) {
        break;
      }
      long id = entity.getKey().getId();
      String title = (String) entity.getProperty("title");
      String text = (String) entity.getProperty("text");
      long timestamp = (long) entity.getProperty("timestamp");
      String imageBlobstoreKey = (String) entity.getProperty("imageBlobstoreKey");
      String userEmail = (String) entity.getProperty("userEmail");

      Comment comment = new Comment(id, title, text, timestamp, imageBlobstoreKey, userEmail);
      comments.add(comment);
    }

    Gson gson = new Gson();

    response.setContentType("application/json");
    response.getWriter().println(gson.toJson(comments));
  }

  @Override
  public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException{
    // open a connection to UserService API
    UserService userService = UserServiceFactory.getUserService();

    if ( !userService.isUserLoggedIn() ) {
      final String loginUrl = userService.createLoginURL("/comments");
      response.sendRedirect(loginUrl);
      return;
    }
      
    String title = getParameter(request, "title", "");
    String text = getParameter(request, "text", "");
    String userEmail = userService.getCurrentUser().getEmail();
    System.out.println(userEmail);
    long timestamp = System.currentTimeMillis();
    
    /* 
     * \\s represents any whitespace character
     * + is a quantifier. it translates to 'one or more'
     * the pattern therefore matches 'one or more whitespace characters'
     */
    String WHITESPACE_REGEX = "\\s+";
    
    /* remove all whitespace from the commentText String */
    String commentTextWhitespaceRemoved = text.replaceAll(WHITESPACE_REGEX, "");
    
    /* if the commentText String, with all whitespace removed, is empty, then the comment is rejected */
    if (commentTextWhitespaceRemoved.equals("")) {
      response.sendError(HttpServletResponse.SC_FORBIDDEN);
    }

    /* Open a connection to blobstore */
    BlobstoreService blobstoreService = BlobstoreServiceFactory.getBlobstoreService();
    /* Get a list of all files uploaded to blobstore from this request */
    Map<String, List<BlobKey>> blobs = blobstoreService.getUploads(request);
    /* Get the blobKey associated with the image uploaded */
    List<BlobKey> blobKeys = blobs.get("imageURL");

    String blobKey = NO_IMAGE_UPLOAD;

    // if a file was uploaded
    if( blobKeys != null && !blobKeys.isEmpty() ) {
      // the form only contains a single file input, so get the first key
      blobKey = blobKeys.get(0).getKeyString();
    }

    Entity commentEntity = new Entity("Comment");
    commentEntity.setProperty("title", title);
    commentEntity.setProperty("text", text);
    commentEntity.setProperty("timestamp", timestamp);
    commentEntity.setProperty("imageBlobstoreKey", blobKey);
    commentEntity.setProperty("userEmail", userEmail);


    DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
    datastore.put(commentEntity);

    response.sendRedirect("/index.html");
  }

  /**
   * @return the request parameter, or the default value if the parameter
   *         was not specified by the client
   */
  private String getParameter(HttpServletRequest request, String name, String defaultValue) {
    String value = request.getParameter(name);
    if (value == null) {
      return defaultValue;
    }
    return value;
  }
}
