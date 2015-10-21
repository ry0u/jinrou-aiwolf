package org.aiwolf.Kog;

import java.util.*;

import org.aiwolf.client.base.player.AbstractWerewolf;
import org.aiwolf.client.lib.TemplateTalkFactory;
import org.aiwolf.client.lib.Topic;
import org.aiwolf.client.lib.Utterance;
import org.aiwolf.common.data.*;
import org.aiwolf.common.net.GameInfo;
import org.aiwolf.common.net.GameSetting;

public class KogWolfPlayer extends AbstractWerewolf{

    int comingoutDay;

    boolean isCameout;
    boolean isSaidAllFakeResult;

    ArrayList<Judge> declaredFakeJudgedAgentList = new ArrayList();
    List<Judge> judgeList = new ArrayList<>();
    HashMap<Agent, Role> comingoutRole = new HashMap<>();

    Agent planningVoteAgent;
    Agent declaredPlanningVoteAgent;
    Agent maybePossesedAgent = null;

    int readTalkListNum;
    Role fakeRole;
    List<Judge> fakeJudgeList = new ArrayList();

    public KogWolfPlayer() {
    }

    public void initialize(GameInfo gameInfo, GameSetting gameSetting) {
        super.initialize(gameInfo, gameSetting);
        ArrayList fakeRoles = new ArrayList(gameSetting.getRoleNumMap().keySet());
        List nonFakeRoleList = Arrays.asList(new Role[]{Role.BODYGUARD, Role.POSSESSED, Role.WEREWOLF});
        fakeRoles.removeAll(nonFakeRoleList);
        this.fakeRole = (Role)fakeRoles.get((new Random()).nextInt(fakeRoles.size()));
        this.comingoutDay = (new Random()).nextInt(3) + 1;
        if(this.fakeRole == Role.VILLAGER) {
            this.comingoutDay = 1000;
        }

        this.isCameout = false;
    }

    public void dayStart() {
        this.declaredPlanningVoteAgent = null;
        this.planningVoteAgent = null;
        this.setPlanningVoteAgent();
        if(this.getDay() >= 1) {
            this.setFakeResult();
        }

        this.isSaidAllFakeResult = false;
        this.readTalkListNum = 0;
    }

    public String talk() {
        String string2;
        if(!this.isCameout && this.getDay() >= this.comingoutDay) {
            string2 = TemplateTalkFactory.comingout(this.getMe(), this.fakeRole);
            this.isCameout = true;
            return string2;
        } else {
            if(this.isCameout && !this.isSaidAllFakeResult) {
                Iterator var2 = this.getMyFakeJudgeList().iterator();

                while(var2.hasNext()) {
                    Judge string = (Judge)var2.next();
                    if(!this.declaredFakeJudgedAgentList.contains(string)) {
                        String string1;
                        if(this.fakeRole == Role.SEER) {
                            string1 = TemplateTalkFactory.divined(string.getTarget(), string.getResult());
                            this.declaredFakeJudgedAgentList.add(string);
                            return string1;
                        }

                        if(this.fakeRole == Role.MEDIUM) {
                            string1 = TemplateTalkFactory.inquested(string.getTarget(), string.getResult());
                            this.declaredFakeJudgedAgentList.add(string);
                            return string1;
                        }
                    }
                }

                this.isSaidAllFakeResult = true;
            }

            if(this.declaredPlanningVoteAgent != this.planningVoteAgent) {
                string2 = TemplateTalkFactory.vote(this.planningVoteAgent);
                this.declaredPlanningVoteAgent = this.planningVoteAgent;
                return string2;
            } else {
                return "Over";
            }
        }
    }

    public String whisper() {
        return TemplateTalkFactory.over();
    }

    public Agent vote() {
        return this.planningVoteAgent;
    }

    public Agent attack() {
        List aliveAgentList = this.getLatestDayGameInfo().getAliveAgentList();
        aliveAgentList.removeAll(this.getWolfList());
        aliveAgentList.remove(this.maybePossesedAgent);
        ArrayList attackCandidatePlayer = new ArrayList();
        Iterator attackAgent = aliveAgentList.iterator();

        while(attackAgent.hasNext()) {
            Agent rand = (Agent)attackAgent.next();
            if(this.comingoutRole.containsKey(rand)) {
                attackCandidatePlayer.add(rand);
            }
        }

        Random rand1 = new Random();
        Agent attackAgent1;
        if(attackCandidatePlayer.size() > 0 && Math.random() < 0.8D) {
            attackAgent1 = (Agent)attackCandidatePlayer.get(rand1.nextInt(attackCandidatePlayer.size()));
        } else {
            attackAgent1 = (Agent)aliveAgentList.get(rand1.nextInt(aliveAgentList.size()));
        }

        return attackAgent1;
    }

    public void finish() {
    }

    public void setPlanningVoteAgent() {
        List aliveAgentList = this.getLatestDayGameInfo().getAliveAgentList();
        aliveAgentList.removeAll(this.getWolfList());
        aliveAgentList.remove(this.maybePossesedAgent);
        if(this.fakeRole == Role.VILLAGER) {
            if(aliveAgentList.contains(this.planningVoteAgent)) {
                return;
            }

            Random fakeHumanList = new Random();
            this.planningVoteAgent = (Agent)aliveAgentList.get(fakeHumanList.nextInt(aliveAgentList.size()));
        }

        ArrayList fakeHumanList1 = new ArrayList();
        ArrayList voteAgentCandidate = new ArrayList();
        Iterator rand = aliveAgentList.iterator();

        while(rand.hasNext()) {
            Agent aliveAgentExceptHumanList = (Agent)rand.next();
            if(this.comingoutRole.containsKey(aliveAgentExceptHumanList) && this.comingoutRole.get(aliveAgentExceptHumanList) == this.fakeRole) {
                voteAgentCandidate.add(aliveAgentExceptHumanList);
            }
        }

        rand = this.getMyFakeJudgeList().iterator();

        while(rand.hasNext()) {
            Judge aliveAgentExceptHumanList1 = (Judge)rand.next();
            if(aliveAgentExceptHumanList1.getResult() == Species.HUMAN) {
                fakeHumanList1.add(aliveAgentExceptHumanList1.getTarget());
            } else {
                voteAgentCandidate.add(aliveAgentExceptHumanList1.getTarget());
            }
        }

        if(!voteAgentCandidate.contains(this.planningVoteAgent)) {
            if(voteAgentCandidate.size() > 0) {
                Random aliveAgentExceptHumanList2 = new Random();
                this.planningVoteAgent = (Agent)voteAgentCandidate.get(aliveAgentExceptHumanList2.nextInt(voteAgentCandidate.size()));
            } else {
                List aliveAgentExceptHumanList3 = this.getLatestDayGameInfo().getAliveAgentList();
                aliveAgentExceptHumanList3.removeAll(fakeHumanList1);
                Random rand1;
                if(aliveAgentExceptHumanList3.size() > 0) {
                    rand1 = new Random();
                    this.planningVoteAgent = (Agent)aliveAgentExceptHumanList3.get(rand1.nextInt(aliveAgentExceptHumanList3.size()));
                } else {
                    rand1 = new Random();
                    this.planningVoteAgent = (Agent)aliveAgentList.get(rand1.nextInt(aliveAgentList.size()));
                }
            }

        }
    }

    public void update(GameInfo gameInfo) {
        super.update(gameInfo);
        List talkList = gameInfo.getTalkList();

        for(int i = this.readTalkListNum; i < talkList.size(); ++i) {
            Talk talk = (Talk)talkList.get(i);
            Utterance utterance = new Utterance(talk.getContent());

            if(utterance.getTopic().equals(Topic.COMINGOUT)) {
                this.comingoutRole.put(talk.getAgent(), utterance.getRole());
                if(utterance.getRole().equals(this.fakeRole)) {
                    this.setPlanningVoteAgent();
                }
            }

            if(utterance.getTopic().equals(Topic.DIVINED)) {
                Judge judge = new Judge(this.getDay(), talk.getAgent(), utterance.getTarget(), utterance.getResult());
                this.judgeList.add(judge);

                if(!this.getWolfList().contains(judge.getAgent())) {
                    Species judgeSpecies = judge.getResult();
                    Species realSpecies;

                    if(this.getWolfList().contains(judge.getAgent())) {
                        realSpecies = Species.WEREWOLF;
                    } else {
                        realSpecies = Species.HUMAN;
                    }

                    if(judgeSpecies != realSpecies) {
                        this.maybePossesedAgent = judge.getAgent();
                        this.setPlanningVoteAgent();
                    }
                }
            }
        }

        this.readTalkListNum = talkList.size();
    }

    public void setFakeResult() {
        ArrayList fakeGiftTargetCandidateList = new ArrayList();
        if(this.fakeRole != Role.VILLAGER) {
            Agent fakeGiftTarget;
            Species fakeResult;
            if(this.fakeRole == Role.SEER) {
                List aliveAgentList = this.getLatestDayGameInfo().getAliveAgentList();
                aliveAgentList.remove(this.getMe());
                Iterator var6 = aliveAgentList.iterator();

                while(var6.hasNext()) {
                    Agent rand = (Agent)var6.next();
                    if(!this.isJudgedAgent(rand) && this.fakeRole != this.comingoutRole.get(rand)) {
                        fakeGiftTargetCandidateList.add(rand);
                    }
                }

                Random rand1;
                if(fakeGiftTargetCandidateList.size() > 0) {
                    rand1 = new Random();
                    fakeGiftTarget = (Agent)fakeGiftTargetCandidateList.get(rand1.nextInt(fakeGiftTargetCandidateList.size()));
                } else {
                    aliveAgentList.removeAll(fakeGiftTargetCandidateList);
                    rand1 = new Random();
                    fakeGiftTarget = (Agent)aliveAgentList.get(rand1.nextInt(aliveAgentList.size()));
                }

                if(this.getWolfList().contains(fakeGiftTarget)) {
                    fakeResult = Species.HUMAN;
                } else if(fakeGiftTarget != this.maybePossesedAgent && this.comingoutRole.containsKey(fakeGiftTarget)) {
                    fakeResult = Species.WEREWOLF;
                } else if(Math.random() < 0.5D) {
                    fakeResult = Species.WEREWOLF;
                } else {
                    fakeResult = Species.HUMAN;
                }
            } else {
                if(this.fakeRole != Role.MEDIUM) {
                    return;
                }

                fakeGiftTarget = this.getLatestDayGameInfo().getExecutedAgent();
                if(fakeGiftTarget == null) {
                    return;
                }

                if(this.getWolfList().contains(fakeGiftTarget)) {
                    fakeResult = Species.HUMAN;
                } else if(fakeGiftTarget != this.maybePossesedAgent && this.comingoutRole.containsKey(fakeGiftTarget)) {
                    fakeResult = Species.WEREWOLF;
                } else if(Math.random() < 0.5D) {
                    fakeResult = Species.WEREWOLF;
                } else {
                    fakeResult = Species.HUMAN;
                }
            }

            if(fakeGiftTarget != null) {
                this.fakeJudgeList.add(new Judge(this.getDay(), this.getMe(), fakeGiftTarget, fakeResult));
            }

        }
    }

    public List<Judge> getMyFakeJudgeList() {
        return this.fakeJudgeList;
    }

    public boolean isJudgedAgent(Agent agent) {
        Iterator var3 = this.getMyFakeJudgeList().iterator();

        while(var3.hasNext()) {
            Judge judge = (Judge)var3.next();
            if(judge.getAgent() == agent) {
                return true;
            }
        }

        return false;
    }
}
