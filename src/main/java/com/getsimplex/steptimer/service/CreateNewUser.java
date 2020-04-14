package com.getsimplex.steptimer.service;

import com.google.gson.Gson;
import com.getsimplex.steptimer.model.User;
import spark.Request;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import com.getsimplex.steptimer.utils.*;

import java.util.function.Predicate;

/**
 * Created by Mandy on 10/4/2016.
 */
public class CreateNewUser {

    private static Gson gson = new Gson();
    private static JedisClient jedisClient = new JedisClient();

    public static String handleRequest(Request request) throws Exception{
        String newUserRequest = request.body();
        User createUser = gson.fromJson(newUserRequest, User.class);
        User addUser = new User();

        String userName = createUser.getUserName();
        String password = createUser.getPassword();
        String verifyPassword = createUser.getVerifyPassword();
        String email = createUser.getEmail();
        String phone = createUser.getPhone();
        String accttype = createUser.getAccountType();
        String bday = createUser.getBirthDate();
        String deviceId = createUser.getDeviceNickName();


        if (userName != null && !userName.isEmpty()) {

            ArrayList<User> users = JedisData.getEntityList(User.class);
            Predicate<User> userPredicate = user -> user.getUserName().equals(userName);
            Optional<User> userOptional = users.stream().filter(userPredicate).findFirst();
            if (userOptional.isPresent()) {
                User currentUser = userOptional.get();
                String newUserName = currentUser.getUserName();

                if (newUserName.equals(userName)) {
                    throw new Exception("Username already exists");
                } else {
                    addUser.setUserName(userName);
                }
            }else {
                addUser.setUserName(userName);
            }
        }

        if(!password.equals(verifyPassword)){ //add validation - create method complexPW
            throw new Exception("Passwords do not match");
        }else if(!validatePassword(password)) {
            throw new Exception ("Your password must contain a lowercase and uppercase letter, a number, a special character, and be at least 6 characters long.");
        }else{
            String newPw = JasyptPwSecurity.encrypt(password);
            addUser.setPassword(newPw);
        }

        if (email != null && !email.isEmpty()) {
                addUser.setEmail(email);
        }

        if (phone != null && !phone.isEmpty()) {

            addUser.setPhone(phone);
        }


        if (bday != null ){
            String birthdate = bday;
            addUser.setBirthDate(birthdate);
        }

        if (accttype != null && !accttype.isEmpty()) {
            addUser.setAccountType(accttype);
        }

        if (deviceId != null && !deviceId.isEmpty()){
            addUser.setDeviceNickName(deviceId);
        }


        //SAVE USER TO REDIS
        JedisData.loadToJedis(addUser,User.class);

        return "Welcome: " + addUser.getUserName() + " Your account has been created, please login.";

    }

    // code borrowed from http://www.java2novice.com/java-collections-and-util/regex/valid-password/
    // must contain one digit, one lower case char, one upper case char, some special chars, length should be within 6 to 15 chars.

    private static Pattern pswPtrn =
            Pattern.compile("((?=.*\\d)(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%!]).{6,15})");

    public static boolean validatePassword(String password){

        Matcher mtch = pswPtrn.matcher(password);
        if(mtch.matches()){
            return true;
        }
        return false;
    }
}
