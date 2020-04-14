package com.getsimplex.steptimer.service;

import com.getsimplex.steptimer.model.LoginToken;
import com.getsimplex.steptimer.model.User;
import com.getsimplex.steptimer.utils.JedisData;

import java.util.ArrayList;
import java.util.Date;
import java.util.Optional;
import java.util.function.Predicate;

/**
 * Created by sean on 6/13/2017.
 */
public class TokenService {

    public static Optional<User> getUserFromToken(String usertoken) throws Exception{
        Optional<User> foundUser = Optional.empty();

        ArrayList<LoginToken> allTokens = JedisData.getEntityList(LoginToken.class);
        Predicate<LoginToken> tokenPredicate = token -> token.getUuid().equals(usertoken);
        Predicate<LoginToken> activePredicate = active -> active.getExpires() && active.getExpiration().after(new Date());
        Optional<LoginToken> tokenOptional = allTokens.stream().filter(tokenPredicate).filter(activePredicate).findFirst();

        if (tokenOptional.isPresent()){
            LoginToken loginToken = tokenOptional.get();
            foundUser = Optional.of(UserService.getUser(loginToken.getUser()));
        }
        return foundUser;
    }
}
