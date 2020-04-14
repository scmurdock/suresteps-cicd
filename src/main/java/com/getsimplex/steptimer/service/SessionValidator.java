package com.getsimplex.steptimer.service;

import com.getsimplex.steptimer.model.LoginToken;
import com.getsimplex.steptimer.utils.JedisData;
import com.getsimplex.steptimer.model.Token;
import com.getsimplex.steptimer.model.ValidationResponse;
import org.eclipse.jetty.websocket.api.Session;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Optional;
import java.util.function.Predicate;

/**
 * Created by sean on 8/12/2016.
 */
public class SessionValidator {

    public static HashMap<org.eclipse.jetty.websocket.api.Session, String> sessionTokens = new HashMap<org.eclipse.jetty.websocket.api.Session, String>();


    public static ValidationResponse validateMachineSession(String sessionToken, Session session) throws Exception{
        ValidationResponse validationResponse = new ValidationResponse();
        Token token = getMachineToken(sessionToken);
        if (token!=null) {
            validationResponse.setOriginType(token.getOriginType());
            if (token.getExpires() && token.getExpiration().toInstant().toEpochMilli()<System.currentTimeMillis()){
                validationResponse.setExpired(true);
            } else{
                validationResponse.setExpired(false);
            }

            if (token.getUser()!=null){
                validationResponse.setUser(token.getUser());
            }

        } else{
            validationResponse.setTrusted(false);
        }

        validationResponse.setOriginIpAddress(session.getRemoteAddress().toString());

        return validationResponse;

    }

    public static Token getMachineToken(String tokenValue) throws Exception{
        Token token = JedisData.get(tokenValue, Token.class);
        return token;
    }

    public static Boolean validateToken(String tokenString) throws Exception{
        ArrayList<LoginToken> allTokens = JedisData.getEntityList(LoginToken.class);
        Predicate<LoginToken> tokenPredicate = token -> token.getUuid().equals(tokenString);
        Optional<LoginToken> matchingToken=allTokens.stream().filter(tokenPredicate).findFirst();

        Boolean expired = false;
        if (matchingToken.isPresent() && matchingToken.get().getExpires() && matchingToken.get().getExpiration().before(new Date())){
            expired = true;
        }

        return expired;
    }



}
