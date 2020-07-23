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

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.gson.Gson;
import java.io.IOException;
import java.util.stream.Collectors;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/** Servlet that returns a programmable number of comments */
@WebServlet("/delete-comment")
public class DeletionServlet extends HttpServlet {

  /**
    * POST handler for the /delete-comment route
    */
  @Override
  public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException{
    // get the body of the request
    String bodyString =
        request
            .getReader()
                .lines()
                    .collect(
                        Collectors.joining(System.lineSeparator())
                    );

    Gson gson = new Gson();
    DeletionRequestBody body =
        gson.fromJson(bodyString, DeletionRequestBody.class);

    DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();

    // each key has a unique id associated with it
    // this id can be used to reconstruct the key using .createKey()
    Key key = KeyFactory.createKey("Comment", body.getId());

    datastore.delete(key);
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

  /**
    * The body of a request sent to the deletion servlet
    */
  private class DeletionRequestBody {
    // the only parameter in the body is the id of the comment to be deleted
    private long id;

    public long getId() {
      return this.id;
    }

    public String toString() {
      return String.format("DeletionRequestBody{ id=\"%d\" }", this.id);
    }
  }
}
