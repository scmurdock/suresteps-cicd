package com.getsimplex.steptimer.service;

import com.getsimplex.steptimer.model.LoginToken;
import com.getsimplex.steptimer.model.Token;
import com.getsimplex.steptimer.model.User;
import com.getsimplex.steptimer.utils.JedisData;
import com.google.gson.Gson;
import spark.Request;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import static com.getsimplex.steptimer.utils.JedisData.deleteFromRedis;

/**
 * Created by Administrator on 12/7/2016.
 */
public class TokenController {

    private static Gson gson = new Gson();


    private static ArrayList<User> allUsers = JedisData.getEntityList(User.class);

    public static Optional<LoginToken> lookupToken(String testToken)throws Exception {
        ArrayList<LoginToken> allTokens = JedisData.getEntityList(LoginToken.class);
        Predicate<LoginToken> tokenPredicate = token -> token.getUuid().equals(testToken);
        Predicate<LoginToken> expirePredicate = expire -> expire.getExpires() && expire.getExpiration().before(new Date());
        Predicate<LoginToken> activePredicate = active -> active.getExpires() && active.getExpiration().after(new Date());

        //        Deletes all expired tokens
        List<LoginToken> expiredTokens = allTokens.stream().filter(tokenPredicate).filter(expirePredicate).collect(Collectors.toList());
        if (!expiredTokens.isEmpty()) {
            deleteFromRedis(expiredTokens);
        }

//        finds active token and returns it
        Optional<LoginToken> tokenOptional = allTokens.stream().filter(tokenPredicate).filter(activePredicate).findFirst();
        if (tokenOptional.isPresent()) {
            LoginToken loginToken = tokenOptional.get();
            return Optional.of(loginToken);
        } else {
            return Optional.empty();
        }

    }

    public static String createUserToken(String userName)throws Exception{
        ArrayList<User> allUsers = JedisData.getEntityList(User.class);
        Predicate<User> userPredicate = user -> user.getUserName().equals(userName);
        Predicate<User> personalTypePredicate = personal -> personal.getAccountType().equals("personal");
//        Predicate<User> businessTypePredicate = business -> business.getAccountType().equals("Business");

        Optional<User> personalOptional = allUsers.stream().filter(userPredicate).filter(personalTypePredicate).findFirst();
//        Optional<User> businessOptional = allUsers.stream().filter(userPredicate).filter(businessTypePredicate).findFirst();

        String tokenString = UUID.randomUUID().toString();
        Long currentTimeMillis = System.currentTimeMillis();
        LoginToken token = new LoginToken();
        token.setExpires(true);
        token.setUuid(tokenString);
        token.setUser(userName);

        if (personalOptional.isPresent()) {
            Long expiration = currentTimeMillis + 10 * 60 * 1000;  // expires after 10 minutes
            Date expirationDate = new Date(expiration);
            token.setExpiration(expirationDate);

        }else{
            Long expiration = currentTimeMillis + 60 * 60 * 1000;  // expires after 1 hours (business account)
            Date expirationDate = new Date(expiration);
            token.setExpiration(expirationDate);
        }
        JedisData.loadToJedis(token, LoginToken.class);
        return tokenString;

    }
}
