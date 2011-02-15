/* Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.protocol.ajp;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

/*
 * Allow to test AJP of mod_jk/mod_proxy_ajp.
 */
class Serv {
  public static void main(String[] argv) {
    ServerSocket serverSocket = null;
	try {
		serverSocket = new ServerSocket(8009);
	} catch (IOException e1) {
		System.out.println("ServerSocket failed: " + e1);
		System.exit(-1);
	}
    Socket clientSocket = null;

    try {
            clientSocket = serverSocket.accept();
    } catch (IOException e) {
            System.out.println("Accept failed");
            System.exit(-1);
    }
    AjpMessageReader ajp = null;
    try {
		ajp = new AjpMessageReader(clientSocket.getInputStream());
	} catch (IOException e) {
		System.out.println("AjpMessageReader failed");
		System.exit(-1);
	} catch (Exception e) {
		System.out.println("AjpMessageReader failed " + e);
	    System.exit(-1);
	}
	System.out.println(ajp.dump());
	/* Normal behaviour 
	if (ajp.getByte() == Ajp.CPING_REQUEST) {
		CpongMessage msg = new CpongMessage();
		try {
			clientSocket.getOutputStream().write(msg.buf, 0, msg.len);
		} catch (IOException e) {
			System.out.println("write failed: " + e);
		}
	}
	*/
	if (ajp.getByte() == Ajp.CPING_REQUEST) {
		SendBodyChunkMessage msg = new SendBodyChunkMessage("12345BAD ME!!!".getBytes());
		try {
			clientSocket.getOutputStream().write(msg.buf, 0, msg.size);
			System.out.println("sending: " + msg.len);
		} catch (IOException e) {
			System.out.println("write failed: " + e);
		}
	}	
  }
}
