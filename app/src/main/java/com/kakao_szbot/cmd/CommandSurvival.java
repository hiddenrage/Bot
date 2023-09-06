package com.kakao_szbot.cmd;

import static com.kakao_szbot.KakaoNotificationListener.KakaoSendReply;
import static com.kakao_szbot.KakaoNotificationListener.getSbn;
import static com.kakao_szbot.cmd.MainCommandChecker.checkCommnadList;
import static com.kakao_szbot.lib.CommonLibrary.patternIndexOf;
import static com.kakao_szbot.lib.CommonLibrary.patternLastIndexOf;

import android.service.notification.StatusBarNotification;
import android.util.Log;

import com.kakao_szbot.lib.FileLibrary;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class CommandSurvival {
    public final static String TAG = "CommandSurvival";

    public static String[] SURVIVAL_SUMMON_CMD = {
            "소환"
    };

    public static String[] SURVIVAL_SKILL_CMD = {
            "바위", "보", "가위", "체력"
    };

    public static String[] SURVIVAL_INFO_CMD = {
            "정보"
    };
    public static String[] SURVIVAL_HELP_CMD = {
            "도움말", "헬프", "help", "-h"
    };
    public static String[] SURVIVAL_RULE_CMD = {
            "룰", "규칙"
    };
    public static String[] SURVIVAL_START_CMD = {
            "시작"
    };

    private static String SURVIVAL_DATA_BASE = "survivalData.csv";
    private static int SURVIVAL_STAT_POINT_MAX = 10;

    private static int SURVIVAL_ATTACK_ROCK = 0;
    private static int SURVIVAL_ATTACK_PAPER = 1;
    private static int SURVIVAL_ATTACK_SCISSORS = 2;
    private static int SURVIVAL_ATTACK_MAX = 3;
    private static String[] SURVIVAL_ATTACK_EMOJI = {"\uD83D\uDC4A", "\uD83D\uDD90", "\uD83D\uDC49"};

    private static int SURVIVAL_BATTLE_DRAW = 0;
    private static int SURVIVAL_BATTLE_FRONT_WIN = 1;
    private static int SURVIVAL_BATTLE_BACK_WIN = 2;
    private static int[][] SURVIVAL_BATTLE_RESULT_ARRAY = {
            {SURVIVAL_BATTLE_DRAW, SURVIVAL_BATTLE_BACK_WIN, SURVIVAL_BATTLE_FRONT_WIN},
            {SURVIVAL_BATTLE_FRONT_WIN, SURVIVAL_BATTLE_DRAW, SURVIVAL_BATTLE_BACK_WIN},
            {SURVIVAL_BATTLE_BACK_WIN, SURVIVAL_BATTLE_FRONT_WIN, SURVIVAL_BATTLE_DRAW}
    };


    private static int SURVIVAL_DEAD = 0;
    private static int SURVIVAL_ALIVE = 1;
    private static int SURVIVAL_MSG_MAX = 99999;

    //public static int total_survant_num = 0;
    public static int survival_start = 0;
    public static int total_battle_num = 0;
    private static int current_hoshi_num = 0;

    /*
    public static List<String> player = new ArrayList<String>();
    public static List<String> survant = new ArrayList<String>();
    public static List<Integer> survant_health = new ArrayList<Integer>();
    public static List<Integer>[] attack_damage = new ArrayList[SURVIVAL_ATTACK_MAX];
    public static List<String>[] attack_name = new ArrayList[SURVIVAL_ATTACK_MAX];
    public static List<Integer> battle_survive = new ArrayList<Integer>();
    public static List<Integer> battle_select = new ArrayList<Integer>();
    public static List<Integer> player_hoshi_num = new ArrayList<Integer>();
    */

    class SurvivalPlayer {
        private String player_name;
        private String survant_name;
        private Integer survant_health;
        private int[] attack_damage = new int[SURVIVAL_ATTACK_MAX];
        private String[] attack_name = new String[SURVIVAL_ATTACK_MAX];
        private int battle_survive;
        private int battle_group;
        private int hoshi_num;
    }

    private static List<SurvivalPlayer> player_list = new ArrayList<SurvivalPlayer>();

    public String helpMessage() {
        String replyMessage =
                "[명령어 종류]\n" +
                " - 도움말\n" +
                " - 소환\n" +
                " - 룰, 규칙\n" +
                " - 정보\n" +
                " - 가위, 바위, 보, 체력";
        return replyMessage;
    }

    public String defaultMessage() {
        String replyMessage = "[2023 추석 이벤트 - 최애의 전쟁]\n" +
                " - 본 이벤트는 서바이벌 컨셉으로 기획되었습니다.\n" +
                " - 참가 신청은 이 곳에 '소환' 이라고만 입력해 주시면 됩니다.\n" +
                " - 꼭 다덕임 오픈채팅방에서 사용중인 프로필로 설정 후에 소환 부탁드립니다.\n\n" +
                helpMessage();
        return replyMessage;
    }

    private String skillhelpMessage() {
        return "※ 스텟 포인트 분배법\n" +
                " - 체력 업/다운, 가위/바위/보 업/다운\n" +
                "   ( 예시 : 가위 업 )\n\n" +
                "※ 공격 이름 설정법\n" +
                " - 가위/바위/보 [원하는 공격 이름]\n" +
                "   ( 예시 : 가위 [용사 펀치!] )";
    }

    private String detailhelpMessage(String sender) {
        String player_name = null;
        int index;

        player_name = getPlayerName(sender);
        if (player_name == null)
            return "개발자에게 문의 바랍니다.\n" +
                    "(프로필 형식을 안맞춰서 실패했을 가능성이 많습니다!)";

        if (getSurvivalPlayer(player_name) == null)
            return "소환 먼저 부탁드립니다.";

        String replyMessage = "[최애의 전쟁 룰북]\n" +
                " - ⭐(호시)가 많은 사람이 최종 승자\n" +
                " - 호시 획득 조건 : \n   > 서번트의 전투 승리 (+?)\n   > 매 전투마다 승자 맞추기 (+?)\n\n" +
                " - 모든 서번트는 랜덤 가위/바위/보로 승부\n" +
                " - 이기면 해당 공격력 만큼 데미지\n" +
                " - 무승부는 무효\n\n" +
                " - 추석 당일(9~18시) 동안 단톡방에서 토너먼트 형식으로 모든 전투는 자동으로 진행\n" +
                " - 기본 잔여 스텟 포인트 2개 분배 필요\n" +
                " - 토너먼트 시작 후에는 스텟 수정 불가\n\n" +
                skillhelpMessage();
        return replyMessage;
    }

    private String getSurvantInfo(SurvivalPlayer player) {
        String replyMessage = "[" + player.player_name + "님의 서번트 정보]\n" +
                " - 서번트 : " + player.survant_name + "\n" +
                " - 체력 : " + player.survant_health + "\n" +
                " - " + SURVIVAL_ATTACK_EMOJI[SURVIVAL_ATTACK_ROCK] + "(" + player.attack_name[SURVIVAL_ATTACK_ROCK] + ") : " + player.attack_damage[SURVIVAL_ATTACK_ROCK] + "\n" +
                " - " + SURVIVAL_ATTACK_EMOJI[SURVIVAL_ATTACK_PAPER] + "(" + player.attack_name[SURVIVAL_ATTACK_PAPER] + ") : " + player.attack_damage[SURVIVAL_ATTACK_PAPER] + "\n" +
                " - " + SURVIVAL_ATTACK_EMOJI[SURVIVAL_ATTACK_SCISSORS] + "(" + player.attack_name[SURVIVAL_ATTACK_SCISSORS] + ") : " + player.attack_damage[SURVIVAL_ATTACK_SCISSORS] + "\n";

        if (survival_start == 1) {
            replyMessage += " - ⭐(호시) : " + player.hoshi_num;
        } else {
            replyMessage += " - 잔여 스텟 포인트 : " + getStatPoint(player);
        }

        return replyMessage;
    }

    private String getSurvantSummaryInfo(SurvivalPlayer player) {
        String replyMessage = player.survant_name + "\n" +
                "\uD83D\uDC95(" + player.survant_health + "), " +
                SURVIVAL_ATTACK_EMOJI[SURVIVAL_ATTACK_ROCK] + "(" + player.attack_damage[SURVIVAL_ATTACK_ROCK] + "), " +
                SURVIVAL_ATTACK_EMOJI[SURVIVAL_ATTACK_PAPER] + "(" + player.attack_damage[SURVIVAL_ATTACK_PAPER] + "), " +
                SURVIVAL_ATTACK_EMOJI[SURVIVAL_ATTACK_SCISSORS] + "(" + player.attack_damage[SURVIVAL_ATTACK_SCISSORS] + ")";

        return replyMessage;
    }

    private synchronized int getSurvivalStart () {
        return survival_start;
    }

    private synchronized void setSurvivalStart (int start) {
        survival_start = start;
    }

    private int getTotalDamage(SurvivalPlayer player) {
        return player.attack_damage[SURVIVAL_ATTACK_ROCK] +
                player.attack_damage[SURVIVAL_ATTACK_PAPER] +
                player.attack_damage[SURVIVAL_ATTACK_SCISSORS];
    }

    private int getStatPoint(SurvivalPlayer player) {
        int stat_point = SURVIVAL_STAT_POINT_MAX;

        stat_point -= player.survant_health;
        stat_point -= player.attack_damage[SURVIVAL_ATTACK_ROCK];
        stat_point -= player.attack_damage[SURVIVAL_ATTACK_PAPER];
        stat_point -= player.attack_damage[SURVIVAL_ATTACK_SCISSORS];

        return stat_point;
    }

    private String parseSkillName(String msg) {
        int first_msg_start_index = msg.indexOf('[') + 1;
        int first_msg_end_index = msg.indexOf(']');
        if ((first_msg_start_index != -1 && first_msg_end_index != -1) &&
                (first_msg_start_index < first_msg_end_index)) {
            return msg.substring(first_msg_start_index, first_msg_end_index);
        }

        return null;
    }

    private int checkSkillCommand(String msg) {
        int rock_index = msg.indexOf("바위");
        int paper_index = msg.indexOf("보");
        int scissors_index = msg.indexOf("가위");

        if (rock_index < 0 && paper_index < 0 && scissors_index < 0)
            return -1;

        if (rock_index < 0)
            rock_index = SURVIVAL_MSG_MAX;

        if (paper_index < 0)
            paper_index = SURVIVAL_MSG_MAX;

        if (scissors_index < 0)
            scissors_index = SURVIVAL_MSG_MAX;

        if (rock_index < paper_index) {
            if (rock_index < scissors_index)
                return SURVIVAL_ATTACK_ROCK;
        } else {
            if (paper_index < scissors_index)
                return SURVIVAL_ATTACK_PAPER;
        }

        return SURVIVAL_ATTACK_SCISSORS;
    }

    private int setSkill(String msg, SurvivalPlayer player, StatusBarNotification sbn) {
        String skill_name;
        int skill_num;

        skill_num = checkSkillCommand(msg);
        if (skill_num >= 0) {
            skill_name = parseSkillName(msg);
            if (skill_name == null) {
                if (msg.indexOf("업") >= 0) {
                    if (getStatPoint(player) == 0) {
                        KakaoSendReply("잔여 스텟 포인트가 없습니다.", sbn);
                        return 0;
                    }
                    player.attack_damage[skill_num] = player.attack_damage[skill_num] + 1;
                } else if (msg.indexOf("다운") >= 0) {
                    if (player.attack_damage[skill_num] == 0) {
                        KakaoSendReply("공격력은 0 이하가 될 수 없습니다.", sbn);
                        return 0;
                    }
                    if (getTotalDamage(player) == 1) {
                        KakaoSendReply("총 공격력은 1 이상이여야 합니다.", sbn);
                        return 0;
                    }
                    player.attack_damage[skill_num] = player.attack_damage[skill_num] - 1;
                } else {
                    return -1;
                }

                FileLibrary csv = new FileLibrary();
                csv.changeSkillStatSurvivalCSV(SURVIVAL_DATA_BASE,
                        player.player_name,
                        player.attack_damage[skill_num],
                        skill_num);
                KakaoSendReply("공격력 적용 성공했습니다!", sbn);
                return 0;
            }

            if (skill_name.indexOf(",") >= 0) {
                KakaoSendReply("공격 이름에 쉼표(,)는 들어갈 수 없습니다 ㅠ.ㅠ", sbn);
                return -1;
            }

            player.attack_name[skill_num] = skill_name;
            FileLibrary csv = new FileLibrary();
            csv.changeSkillNameSurvivalCSV(SURVIVAL_DATA_BASE,
                    player.player_name,
                    player.attack_name[skill_num],
                    skill_num);
            KakaoSendReply("공격 이름 적용 성공했습니다!", sbn);
            return 0;
        }

        return -1;
    }

    private int setHealth(String msg, SurvivalPlayer player, StatusBarNotification sbn) {
        if (msg.indexOf("체력") < 0) {
            return -1;
        }

        if (msg.indexOf("업") >= 0) {
            if (getStatPoint(player) == 0) {
                KakaoSendReply("잔여 스텟 포인트가 없습니다.", sbn);
                return 0;
            }
            player.survant_health = player.survant_health + 1;
        } else if (msg.indexOf("다운") >= 0) {
            if (player.survant_health == 1) {
                KakaoSendReply("체력은 1 이하가 될 수 없습니다.", sbn);
                return 0;
            }
            player.survant_health = player.survant_health - 1;
        } else {
            return -1;
        }

        FileLibrary csv = new FileLibrary();
        csv.changeHealthSurvivalCSV(SURVIVAL_DATA_BASE,
                player.player_name,
                player.survant_health);
        KakaoSendReply("체력 적용 성공했습니다!", sbn);
        return 0;
    }

    private synchronized String setStatMessage(String msg, String sender, StatusBarNotification sbn) {
        SurvivalPlayer player = null;
        String player_name = null;

        player_name = getPlayerName(sender);
        if (player_name == null)
            return "정보 조회를 실패하였습니다. 개발자에게 문의 바랍니다.\n" +
                    "(프로필 형식을 안맞춰서 실패했을 가능성이 많습니다!)";

        player = getSurvivalPlayer(player_name);
        if (player == null)
            return "소환 먼저 부탁드립니다.";

        if (setSkill(msg, player, sbn) == 0) {
            return getSurvantInfo(player);
        }

        if (setHealth(msg, player, sbn) == 0) {
            return getSurvantInfo(player);
        }

        return "다시 말해주세요.\n" +
                skillhelpMessage();
    }

    private String survantInfoMessage(String sender) {
        SurvivalPlayer player = null;
        String player_name = null;

        player_name = getPlayerName(sender);
        if (player_name == null)
            return "정보 조회를 실패하였습니다. 개발자에게 문의 바랍니다.\n" +
                    "(프로필 형식을 안맞춰서 실패했을 가능성이 많습니다!)";

        player = getSurvivalPlayer(player_name);
        if (player == null)
            return "소환 먼저 부탁드립니다.";

        return getSurvantInfo(player);
    }

    private String getPlayerName(String sender) {
        int patternIndex = 0;

        patternIndex = patternIndexOf(sender, "[0-9`~!@#$%^&*()-_=+\\|\\[\\]{};:'\",.<>/? ]");
        Log.d(TAG, "patternIndexOf : " + patternIndex);
        if (patternIndex != 0)
            return sender.substring(0, patternIndex);

        return null;
    }

    private String getSurvantName(String sender) {
        int patternIndex = 0;

        patternIndex = patternLastIndexOf(sender, "[0-9`~!@#$%^&*()-_=+\\|\\[\\]{};:'\",.<>/? ]");
        Log.d(TAG, "patternLastIndexOf : " + patternIndex);
        if (patternIndex != 0)
            return sender.substring(patternIndex, sender.length());

        return null;
    }

    public synchronized String summonSurvent(String sender, StatusBarNotification sbn) throws InterruptedException {
        String player_name = null, survant_name = null;

        player_name = getPlayerName(sender);
        if (player_name == null)
            return "소환에 실패하였습니다. 개발자에게 문의 바랍니다.\n" +
                    "(프로필 형식을 안맞춰서 실패했을 가능성이 많습니다!)";

        survant_name = getSurvantName(sender);
        if (survant_name == null)
            return "소환에 실패하였습니다. 개발자에게 문의 바랍니다.\n" +
                    "(프로필 형식을 안맞춰서 실패했을 가능성이 많습니다!)";

        if (getSurvivalPlayer(player_name) != null)
            return "이미 소환된 서번트가 있습니다.";

        FileLibrary csv = new FileLibrary();
        csv.WriteSurvivalCSV(SURVIVAL_DATA_BASE, player_name, survant_name, 5, 1, 1, 1);

        SurvivalPlayer player = new SurvivalPlayer();

        player.player_name = player_name;
        player.survant_name = survant_name;
        player.survant_health = 5;
        player.attack_damage[SURVIVAL_ATTACK_ROCK] = 1;
        player.attack_damage[SURVIVAL_ATTACK_PAPER] = 1;
        player.attack_damage[SURVIVAL_ATTACK_SCISSORS] = 1;
        player.attack_name[SURVIVAL_ATTACK_ROCK] = "바위";
        player.attack_name[SURVIVAL_ATTACK_PAPER] = "보";
        player.attack_name[SURVIVAL_ATTACK_SCISSORS] = "가위";
        player.battle_survive = SURVIVAL_ALIVE;
        player.battle_group = 0;
        player.hoshi_num = 0;

        player_list.add(player);

        /*
        player.add(player_name);
        survant.add(survant_name);
        survant_health.add(5);
        attack_damage[SURVIVAL_ATTACK_ROCK].add(1);
        attack_damage[SURVIVAL_ATTACK_PAPER].add(1);
        attack_damage[SURVIVAL_ATTACK_SCISSORS].add(1);
        attack_name[SURVIVAL_ATTACK_ROCK].add("바위");
        attack_name[SURVIVAL_ATTACK_PAPER].add("보");
        attack_name[SURVIVAL_ATTACK_SCISSORS].add("가위");
        battle_survive.add(1);
        battle_select.add(0);
        player_hoshi_num.add(0);
        total_survant_num++;
        */

        KakaoSendReply("\uD83D\uDCAE", sbn);
        Thread.sleep(3000);
        String ment = "\" 서번트 「" + survant_name + "」 소환에 응해 찾아왔습니다. " + player_name + "님, 당신이 나의 마스터입니까? \"";
        KakaoSendReply(ment, sbn);
        Thread.sleep(3000);
        KakaoSendReply(survantInfoMessage(sender), sbn);
        Thread.sleep(3000);
        String result = "최애의 전쟁에 참가하신 것을 환영합니다.\n" +
                "자세한 설명은 '규칙, 룰' 명령어로 확인할 수 있습니다.\n" +
                "먼저 잔여 스텟 포인트를 사용 해주세요.\n\n"
                + skillhelpMessage();

        return result;
    }

    private SurvivalPlayer getSurvivalPlayer(String name) {
        for (SurvivalPlayer p : player_list) {
            if (p.player_name.equals(name)) {
                return p;
            }
        }

        return null;
    }

    private SurvivalPlayer findFrontGroupPlayer(int battle_num) {
        for (SurvivalPlayer p : player_list) {
            if (p.battle_group == battle_num && p.battle_survive == SURVIVAL_ALIVE) {
                return p;
            }
        }

        return null;
    }

    private SurvivalPlayer findBackGroupPlayer(int battle_num) {
        int front_skip = 0;

        for (SurvivalPlayer p : player_list) {
            if (p.battle_group == battle_num && p.battle_survive == SURVIVAL_ALIVE) {
                if (front_skip == 0) {
                    front_skip = 1;
                } else {
                    return p;
                }
            }
        }

        return null;
    }

    private SurvivalPlayer playBattle(int battle_group, int battle_round ,StatusBarNotification sbn) throws InterruptedException {
        int i = 0;
        Random random = new Random();
        SurvivalPlayer player_front, player_back, player_win;
        int rand_front, rand_back;
        int hp_front, hp_back;


        player_front = findFrontGroupPlayer(battle_group);
        player_back = findBackGroupPlayer(battle_group);
        hp_front = player_front.survant_health;
        hp_back = player_back.survant_health;

        current_hoshi_num++;
        String battle_info = "[최애의 전쟁 배틀 시작 준비]\n" +
                "<1번> 서번트 : " + getSurvantSummaryInfo(player_front) +
                "\n\n<2번> 서번트 : " + getSurvantSummaryInfo(player_back) +
                "\n\n잠시 후.. 10분 뒤 전투가 시작됩니다." +
                "\n승리할 것 같은 서번트에게 투표해주세요." +
                "\n\n※ 맞추면 ⭐(+" + current_hoshi_num + "), 틀리면 ⭐(-" + current_hoshi_num + ")" +
                "\n - " + CommandList.BOT_NAME + " 최애 1번 or " + CommandList.BOT_NAME + " 최애 2번";
        KakaoSendReply(battle_info, sbn);
        Thread.sleep(10000);

        /* Battle */
        battle_info = "[전투 상세 내역]\n0. " +
                player_front.survant_name + " VS " + player_back.survant_name;
        while (true) {
            rand_front = random.nextInt(SURVIVAL_ATTACK_MAX);
            rand_back = random.nextInt(SURVIVAL_ATTACK_MAX);

            i++;
            battle_info += "\n" + i + ". ";
            battle_info += SURVIVAL_ATTACK_EMOJI[rand_front] + "(" +
                    player_front.attack_damage[rand_front] + ") VS " +
                    SURVIVAL_ATTACK_EMOJI[rand_back] + "(" +
                    player_back.attack_damage[rand_back] + ") : ";

            int result = SURVIVAL_BATTLE_RESULT_ARRAY[rand_front][rand_back];
            if (result == SURVIVAL_BATTLE_FRONT_WIN) {
                hp_back -= player_front.attack_damage[rand_front];
                battle_info += player_front.survant_name + " 승";
            } else if (result == SURVIVAL_BATTLE_BACK_WIN) {
                hp_front -= player_back.attack_damage[rand_back];
                battle_info += player_back.survant_name + " 승";
            } else {
                battle_info += "무승부";
                /*
                int damage = player_front.attack_damage[rand_front] - player_back.attack_damage[rand_back];
                if (damage > 0) {
                    hp_back -= damage;
                } else if (damage == 0) {
                    //battle_info += "그러나 아무 일도 일어나지 않았다";
                } else {
                    hp_front += damage;
                }
                */
            }

            if (hp_front <= 0) {
                battle_info += "\n\n※ " + player_back.survant_name + "의 「" +
                        player_back.attack_name[rand_back] +
                        "」 최후의 일격!";
                battle_info += "\n - " +
                        player_front.survant_name + " (체력 : " + hp_front + ") 쓰러졌다.";
                player_front.battle_survive = SURVIVAL_DEAD;
                player_win = player_back;
                break;
            }

            if (hp_back <= 0) {
                battle_info += "\n\n※ " + player_front.survant_name + "의 「" +
                        player_front.attack_name[rand_front] +
                        "」 최후의 일격!";
                battle_info += "\n - " +
                        player_back.survant_name + " (체력 : " + hp_back + ") 쓰러졌다.";
                player_back.battle_survive = SURVIVAL_DEAD;
                player_win = player_front;
                break;
            }
        }

        /* Result */
        if (battle_round != 2) {
            battle_info += "\n\n[최애의 전쟁 " + battle_round + "강 - " + i + "라운드 결과]\n" +
                    " - " + player_win.survant_name + " 승리. \uD83C\uDF89";
        } else {
            battle_info += "\n\n[최애의 전쟁 결승전 결과]\n" +
                    " - " + player_win.survant_name +
                    " (마스터 : " + player_win.player_name+ "님) 우승. \uD83C\uDF8A";
        }
        KakaoSendReply(battle_info, sbn);

        return player_win;
    }

    private void selectGroupForceCommand(SurvivalPlayer player, int battle_num) {
        player.battle_group = battle_num;
    }

    private void selectGroupCommand(int battle_num) {
        Random random = new Random();
        int rand;

        rand = random.nextInt(player_list.size());
        while(true) {
            if (player_list.get(rand).battle_group == 0 &&
                    player_list.get(rand).battle_survive == SURVIVAL_ALIVE)
                break;

            rand++;
            if (rand == player_list.size())
                rand = 0;
        }
        player_list.get(rand).battle_group = battle_num;
    }

    private void makeBattleCommand(int battle_num, SurvivalPlayer player) {
        while (battle_num > 0) {
            if (player == null) {
                selectGroupCommand(battle_num);
            } else {
                selectGroupForceCommand(player, battle_num);
                player = null;
            }
            selectGroupCommand(battle_num);

            battle_num--;
        }
    }

    private void makeTournamentCommand(StatusBarNotification sbn) throws InterruptedException {
        int prev_battle_num = player_list.size();
        int temp_battle_num;
        SurvivalPlayer win_by_default = null;

        while (prev_battle_num > 1) {
            temp_battle_num = (prev_battle_num / 2);
            total_battle_num += temp_battle_num;

            if (prev_battle_num != 2 && prev_battle_num % 2 == 1) {
                temp_battle_num++;
            }

            prev_battle_num = temp_battle_num;
        }

        Log.d(TAG, "총 배틀 라운드 수 : " + total_battle_num);

        prev_battle_num = player_list.size();
        while (prev_battle_num > 1) {
            temp_battle_num = (prev_battle_num / 2);

            makeBattleCommand(temp_battle_num, win_by_default);

            String battle_info;
            if (prev_battle_num != 2) {
                battle_info = "[최애의 전쟁 " + prev_battle_num + "강 대진표]\n" +
                        "※ 총 라운드 수 : " + temp_battle_num;

                for (int i = 1; i <= temp_battle_num; i++) {
                    battle_info += "\n - " + i + " 라운드 : " +
                            findFrontGroupPlayer(i).survant_name +
                            " VS " +
                            findBackGroupPlayer(i).survant_name;
                }
            } else {
                battle_info = "[최애의 전쟁 결승전]\n" +
                        findFrontGroupPlayer(1).survant_name +
                        " VS " +
                        findBackGroupPlayer(1).survant_name;
            }

            win_by_default = null;
            if (prev_battle_num % 2 == 1) {
                win_by_default = findFrontGroupPlayer(0);
                battle_info += "\n - 부전승 : " + win_by_default.survant_name;
            }

            KakaoSendReply(battle_info, sbn);
            Thread.sleep(5000);

            if (prev_battle_num != 2) {
                for (int i = 1; i <= temp_battle_num; i++) {
                    SurvivalPlayer win_player = playBattle(i, prev_battle_num, sbn);
                    win_player.hoshi_num = win_player.hoshi_num + current_hoshi_num;
                    Thread.sleep(10000);
                }
            } else {
                SurvivalPlayer win_player = playBattle(1, prev_battle_num, sbn);
                win_player.hoshi_num = win_player.hoshi_num + current_hoshi_num;
            }

            for (int i = 0; i < player_list.size(); i++) {
                player_list.get(i).battle_group = 0;
            }

            if (prev_battle_num % 2 == 1) {
                temp_battle_num++;
            }

            prev_battle_num = temp_battle_num;
        }

        /* clean up */
        for (int i = 0; i < player_list.size(); i++) {
            player_list.get(i).battle_survive = SURVIVAL_ALIVE;
            player_list.get(i).hoshi_num = 0;
        }
        current_hoshi_num = 0;
    }

    public String startSurvivalCommand(StatusBarNotification sbn) throws InterruptedException {
        if (getSurvivalStart() == 1)
            return "최애의 전쟁이 이미 시작 되었습니다.";

        setSurvivalStart(1);
        makeTournamentCommand(sbn);
        setSurvivalStart(0);

        return null;
    }

    public String mainSurvivalCommand(String msg, String sender, StatusBarNotification sbn) {
        try {
            if (checkCommnadList(msg, SURVIVAL_START_CMD) == 0) {
                return startSurvivalCommand(sbn);
            }
            if (checkCommnadList(msg, SURVIVAL_HELP_CMD) == 0) {
                return helpMessage();
            }
            if (checkCommnadList(msg, SURVIVAL_RULE_CMD) == 0) {
                return detailhelpMessage(sender);
            }
            if (checkCommnadList(msg, SURVIVAL_INFO_CMD) == 0) {
                return survantInfoMessage(sender);
            }
            if (checkCommnadList(msg, SURVIVAL_SUMMON_CMD) == 0) {
                if (getSurvivalStart() == 1)
                    return "최애의 전쟁이 시작되어 명령어 실행 불가입니다 ㅠ.ㅠ";

                return summonSurvent(sender, sbn);
            }
            if (checkCommnadList(msg, SURVIVAL_SKILL_CMD) == 0) {
                if (getSurvivalStart() == 1)
                    return "최애의 전쟁이 시작되어 명령어 실행 불가입니다 ㅠ.ㅠ";

                return setStatMessage(msg, sender, sbn);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return defaultMessage();
    }

    public synchronized void loadSurvivalData() {
        /*
        for (int i = 0; i < SURVIVAL_ATTACK_MAX; i++) {
            attack_damage[i] = new ArrayList<Integer>();
            attack_name[i] = new ArrayList<String>();
        }
        */

        FileLibrary csv = new FileLibrary();
        String allData = csv.ReadCSV(SURVIVAL_DATA_BASE);
        if (allData == null)
            return;

        String[] parts = allData.split("\n");
        for (String part : parts) {
            String[] data = part.split(",");

            SurvivalPlayer player = new SurvivalPlayer();

            player.player_name = data[0];
            player.survant_name = data[1];
            player.survant_health = Integer.parseInt(data[2]);
            player.attack_damage[SURVIVAL_ATTACK_ROCK] = Integer.parseInt(data[3]);
            player.attack_damage[SURVIVAL_ATTACK_PAPER] = Integer.parseInt(data[4]);
            player.attack_damage[SURVIVAL_ATTACK_SCISSORS] = Integer.parseInt(data[5]);
            player.attack_name[SURVIVAL_ATTACK_ROCK] = data[6];
            player.attack_name[SURVIVAL_ATTACK_PAPER] = data[7];
            player.attack_name[SURVIVAL_ATTACK_SCISSORS] = data[8];
            player.battle_survive = SURVIVAL_ALIVE;
            player.battle_group = 0;
            player.hoshi_num = 0;

            player_list.add(player);

            /*
            player.add(data[0]);
            survant.add(data[1]);
            survant_health.add(Integer.parseInt(data[2]));
            attack_damage[SURVIVAL_ATTACK_ROCK].add(Integer.parseInt(data[3]));
            attack_damage[SURVIVAL_ATTACK_PAPER].add(Integer.parseInt(data[4]));
            attack_damage[SURVIVAL_ATTACK_SCISSORS].add(Integer.parseInt(data[5]));
            attack_name[SURVIVAL_ATTACK_ROCK].add(data[6]);
            attack_name[SURVIVAL_ATTACK_PAPER].add(data[7]);
            attack_name[SURVIVAL_ATTACK_SCISSORS].add(data[8]);
            battle_survive.add(1);
            battle_select.add(0);
            player_hoshi_num.add(0);
            total_survant_num++;
            */
        }

        Log.d(TAG, "전체 서버트 수 : " + player_list.size());
    }
}
