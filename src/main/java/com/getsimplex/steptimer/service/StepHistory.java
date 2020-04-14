package com.getsimplex.steptimer.service;

import com.getsimplex.steptimer.model.*;
import com.google.gson.Gson;
import spark.Request;
import com.getsimplex.steptimer.utils.GsonFactory;
import com.getsimplex.steptimer.utils.JedisData;

import java.math.BigDecimal;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;


/**
 * Created by .
 */
public class StepHistory {

    private static Gson gson = GsonFactory.getGson();

    public static String saveSensorTail(Request request) throws Exception{
        String tokenString = request.headers("suresteps.session.token");
        Optional<User> user = TokenService.getUserFromToken(tokenString);//

        Boolean tokenExpired = SessionValidator.validateToken(tokenString);

        if (!user.isPresent()){
            throw new Exception("Could not find user with token");
        } else if (tokenExpired.equals(true)){
            throw new Exception("Session expired");
        }

        Tail tail = gson.fromJson(request.body(),Tail.class);
        tail.setSessionId(tokenString);

        JedisData.loadToJedis(tail,Tail.class);

        return tokenString;
    }


    public static String getAllTests(String email) {
        ArrayList<RapidStepTest> allTests = JedisData.getEntityList(RapidStepTest.class);
        Predicate<RapidStepTest> historicUserPredicate = user -> user.getCustomer().getEmail().equals(email);

        List<RapidStepTest> rapidStepTests = allTests.stream().filter(historicUserPredicate).collect(Collectors.toList());
        return (gson.toJson(rapidStepTests));
    }

    public static String riskScore(String email) throws Exception{
        ArrayList<RapidStepTest> allTests = JedisData.getEntityList(RapidStepTest.class);
        Predicate<RapidStepTest> historicUserPredicate = stepTest -> stepTest.getCustomer().getEmail().equals(email);

        List<RapidStepTest> rapidStepTestsSortedByDate = allTests.stream().filter(historicUserPredicate).sorted(Comparator.comparing(RapidStepTest::getStartTime)).collect(Collectors.toList());
        if (rapidStepTestsSortedByDate.size()<2){
            throw new Exception("Customer "+email+" has: "+rapidStepTestsSortedByDate.size()+" rapid step tests on file which is less than the required number(2) to calculate fall risk.");
        }

        RapidStepTest mostRecentTest = rapidStepTestsSortedByDate.get(rapidStepTestsSortedByDate.size()-1);
        RapidStepTest secondMostRecentTest = rapidStepTestsSortedByDate.get(rapidStepTestsSortedByDate.size()-2);

        BigDecimal currentTestAverageScore = BigDecimal.valueOf((mostRecentTest.getStopTime()-mostRecentTest.getStartTime())+ (secondMostRecentTest.getStopTime()-secondMostRecentTest.getStartTime())).divide(BigDecimal.valueOf(2l));

        RapidStepTest thirdMostRecentTest = rapidStepTestsSortedByDate.get(rapidStepTestsSortedByDate.size()-3);
        RapidStepTest fourthMostRecentTest = rapidStepTestsSortedByDate.get(rapidStepTestsSortedByDate.size()-4);

        BigDecimal previousTestAverageScore = BigDecimal.valueOf((thirdMostRecentTest.getStopTime()-thirdMostRecentTest.getStartTime())+ (fourthMostRecentTest.getStopTime()-fourthMostRecentTest.getStartTime())).divide(BigDecimal.valueOf(2l));

        BigDecimal riskScore = (currentTestAverageScore.subtract(previousTestAverageScore)).divide(new BigDecimal(1000l));
        //positive means they have improved
        //negative means they have declined


        return riskScore.setScale(2, BigDecimal.ROUND_HALF_UP).toString();//score of magnitude 10 or larger means significant change in risk
    }

}
