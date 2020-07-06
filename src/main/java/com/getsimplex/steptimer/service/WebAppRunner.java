package com.getsimplex.steptimer.service;

/**
 * Created by sean on 8/10/2016 based on https://github.com/tipsy/spark-websocket/tree/master/src/main/java
 */


import com.getsimplex.steptimer.model.User;
import com.getsimplex.steptimer.utils.*;
import spark.Request;
import spark.Response;
import spark.Spark;

import java.util.Optional;

import static spark.Spark.*;

public class WebAppRunner {

    public static void main(String[] args){

        Spark.port(getHerokuAssignedPort());
		//secure("/Applications/steptimerwebsocket/keystore.jks","password","/Applications/steptimerwebsocket/keystore.jks","password");
        staticFileLocation("/public");
        //webSocket("/socket", WebSocketHandler.class);
        //post("/sensorUpdates", (req, res)-> WebServiceHandler.routeDeviceRequest(req));
        //post("/generateHistoricalGraph", (req, res)->routePdfRequest(req, res));
        //get("/readPdf", (req, res)->routePdfRequest(req, res));
        post("/user", (req, res)-> callUserDatabase(req));
        get ("/stephistory/:customer", (req, res)-> {
            try{
                userFilter(req, res);
            } catch (Exception e){
                return "/";
            }
            return StepHistory.getAllTests(req.params(":customer"));
        });
        post("/customer", (req, res)-> {
            String newLocation;
            try {
                userFilter(req, res);
                createNewCustomer(req, res);
                newLocation="/timer.html";
            } catch (Exception e){
                System.out.println("*** Error Creating Customer: "+e.getMessage());
                newLocation="/";
            }
            return newLocation;
        });
        get("/customer/:customer", (req, res)-> {
            try {
                userFilter(req, res);
                return FindCustomer.handleRequest(req);
            } catch (Exception e){
                System.out.println("*** Error Finding Customer: "+e.getMessage());
                return null;
            }

        });

        post("/login", (req, res)->loginUser(req));
        post("/rapidsteptest", (req, res)->{
            try{
                userFilter(req, res);
            } catch (Exception e){
                return "/";
            }

            saveStepSession(req);
            return "Saved";
        });
        get("/riskscore/:customer",((req,res) -> {
            try{
                userFilter(req, res);
            } catch (Exception e){
                System.out.println("*** Error Finding Risk Score: "+e.getMessage());
                return null;
            }
            return riskScore(req.params(":customer"));
        }));
        //post ("/sensorTail",(req,res) -> saveTail(req,res) );

        init();
    }

    private static void userFilter(Request request, Response response) throws Exception{
        String tokenString = request.headers("suresteps.session.token");

            Optional<User> user = TokenService.getUserFromToken(tokenString);//

            Boolean tokenExpired = SessionValidator.validateToken(tokenString);

            if (!user.isPresent() || tokenExpired.equals(true)) { //Check to see if session expired
                throw new Exception("Invalid user token");
            }

    }

    public static String saveTail(Request req, Response res) throws Exception{
        return StepHistory.saveSensorTail(req);
    }

    public static String routePdfRequest(Request req, Response res) throws Exception{
        return WebServiceHandler.routePdfRequest(req, res);
    }

    public static void createNewCustomer(Request request, Response response) throws Exception{
            CreateNewCustomer.handleRequest(request);
    }

    private static String callUserDatabase(Request request)throws Exception{
        return CreateNewUser.handleRequest(request);
    }

    private static String loginUser(Request request) throws Exception{
        return LoginController.handleRequest(request);

    }

    private static String riskScore(String email) throws Exception{
        return StepHistory.riskScore(email);
    }

    private static void saveStepSession(Request request) throws Exception{
        SaveRapidStepTest.save(request.body());
    }

    public static String authenticateSession(Request request) throws Exception{

        String tokenString = request.headers("suresteps.session.token");

        Optional<User> user = TokenService.getUserFromToken(tokenString);//

        if (!user.isPresent()){
            throw new Exception("Could not find user with token");
        }
        else {
            return user.get().getUserName();
        }

    }
	
    private static int getHerokuAssignedPort() {
        ProcessBuilder processBuilder = new ProcessBuilder();
        if (processBuilder.environment().get("PORT") != null) {
            return Integer.parseInt(processBuilder.environment().get("PORT"));
        }
        return Configuration.getConfiguration().getInt("suresteps.port"); //return default port if heroku-port isn't set (i.e. on localhost)
    }
}
