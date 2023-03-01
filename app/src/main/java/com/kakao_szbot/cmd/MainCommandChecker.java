package com.kakao_szbot.cmd;

import android.content.Context;
import android.util.Log;


public class MainCommandChecker {
    public final static String TAG = "CommandChecker";

    public String checkKakaoMessage(String msg, String sender) {
        Log.d(TAG, "checkKakaoMessage ~ " + sender + ": " + msg);
        String replyMessage = null;


        replyMessage = highPriorityMessage(msg, sender);
        if (replyMessage != null) {
            return replyMessage;
        }

        if (msg.contains(CommandList.BOT_NAME)) {
            replyMessage = selectBotMessage(msg, sender);
        } else {
            replyMessage = selectNormalMessage(msg, sender);
        }

        return replyMessage;
    }

    private String selectBotMessage(String msg, String sender) {
        String replyMessage = null;

        try {
            if (checkCommnadList(msg, CommandList.RAMEN_CMD) == 0) {
                replyMessage = new CommandBasic().ramenMessage(msg);
                return replyMessage;
            }
            if (checkCommnadList(msg, CommandList.LOTTO_CMD) == 0) {
                replyMessage = new CommandBasic().lottoMessage(msg);
                return replyMessage;
            }
            if (checkCommnadList(msg, CommandList.COIN_CMD) == 0) {
                replyMessage = new CommandCrawling().coinMessage(msg, sender);
                return replyMessage;
            }
            if (checkCommnadList(msg, CommandList.WEATHER_CMD) == 0) {
                replyMessage = new CommandCrawling().weatherMessage(msg, sender);
                return replyMessage;
            }
            if (checkCommnadList(msg, CommandList.RECOMMEND_ANI_CMD) == 0) {
                replyMessage = new CommandCrawling().recommendAniMessage(msg, sender);
                return replyMessage;
            }
            if (checkCommnadList(msg, CommandList.TODAY_ANI_CMD) == 0) {
                replyMessage = new CommandCrawling().todayAniMessage(msg, sender);
                return replyMessage;
            }
            if (checkCommnadList(msg, CommandList.STUDY_CMD) == 0) {
                replyMessage = new CommandStudy().studyMessage(msg);
                return replyMessage;
            }
            if (checkCommnadList(msg, CommandList.SAMPLING_CMD) == 0) {
                replyMessage = new CommandSampling().samplingMessage(msg);
                return replyMessage;
            }
            if (checkCommnadList(msg, CommandList.QUIZ_CMD) == 0) {
                replyMessage = new CommandQuiz().quizMessage(msg);
                return replyMessage;
            }
            if (checkCommnadList(msg, CommandList.GACHA_CMD) == 0) {
                replyMessage = new CommandGacha().gachaMessage(msg);
                return replyMessage;
            }
            if (checkCommnadList(msg, CommandList.REINFORCE_CMD) == 0) {
                replyMessage = new CommandReinforce().reinforceMessage(msg);
                return replyMessage;
            }
            if (checkCommnadList(msg, CommandList.INVEST_CMD) == 0) {
                replyMessage = new CommandInvest().investMessage(msg);
                return replyMessage;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        for (int i = 0; i < CommandList.BOT_BASIC_CMD.length; i++) {
            if (checkCommnadList(msg, CommandList.BOT_BASIC_CMD[i]) == 0) {
                replyMessage = new CommandBasic().basicMessage(CommandList.BOT_BASIC_MSG[i]);
                return replyMessage;
            }
        }

        return replyMessage;
    }

    private String selectNormalMessage(String msg, String sender) {
        String replyMessage = null;

        new CommandSampling().storeSamplingMessage(msg);

        for (int i = 0; i < CommandList.COMMON_BASIC_CMD.length; i++) {
            if (checkCommnadList(msg, CommandList.COMMON_BASIC_CMD[i]) == 0) {
                replyMessage = new CommandBasic().sometimesMessage(CommandList.COMMON_BASIC_MSG[i]);
                break;
            }
        }
        if (replyMessage != null)
            return replyMessage;

        replyMessage = new CommandQuiz().answerQuizMessage(msg, sender);
        if (replyMessage != null)
            return replyMessage;

        replyMessage = new CommandStudy().checkStudyMessage(msg);

        return replyMessage;
    }

    private String highPriorityMessage(String msg, String sender) {
        String replyMessage = new CommandBasic().slangMessage(msg, CommandList.SLANG_CMD);
        return replyMessage;
    }

    private int checkCommnadList(String msg, String[] list) {
        for (int i = 0; i < list.length; i++) {
            if (msg.indexOf(list[i]) != -1) {
                return 0;
            }
        }

        return -1;
    }
}