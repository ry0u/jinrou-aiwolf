package org.aiwolf.Kog;

import org.aiwolf.client.base.player.AbstractPossessed;
import org.aiwolf.client.lib.TemplateTalkFactory;
import org.aiwolf.client.lib.Topic;
import org.aiwolf.client.lib.Utterance;
import org.aiwolf.common.data.*;
import org.aiwolf.common.net.GameInfo;
import org.aiwolf.common.net.GameSetting;

import java.util.*;

/**
 * Created by ry0u on 15/08/15.
 */
public class KogPossessedPlayer extends AbstractPossessed {

    int comingoutDay;
    int readTalkListNum;

    boolean isCameout;
    boolean isSaidAllFakeResult;

    ArrayList<Judge> declaredFakeJudgedAgentList = new ArrayList();
    List<Judge> fakeJudgeList = new ArrayList();
    List<Judge> judgeList = new ArrayList();
    Map<Agent, Role> comingoutRole = new HashMap<>();

    Agent planningVoteAgent;
    Agent declaredPlanningVoteAgent;

    Role fakeRole;


    public KogPossessedPlayer() {
    }

    @Override
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

    public Agent vote() {
        return this.planningVoteAgent;
    }

    public void finish() {
    }

    public void setPlanningVoteAgent() {
        List aliveAgentList = this.getLatestDayGameInfo().getAliveAgentList();
        aliveAgentList.remove(this.getMe());
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
        boolean existInspectResult = false;

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
                existInspectResult = true;
            }

        }

        this.readTalkListNum = talkList.size();
        if(existInspectResult) {
            this.setPlanningVoteAgent();
        }

    }

    public void setFakeResult() {
        Agent fakeGiftTarget = null;
        Species fakeResult = null;
        if(this.fakeRole == Role.SEER) {
            ArrayList fakeGiftTargetCandidateList = new ArrayList();
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

            if(Math.random() < 0.3D) {
                fakeResult = Species.WEREWOLF;
            } else {
                fakeResult = Species.HUMAN;
            }
        } else {
            if(this.fakeRole != Role.MEDIUM) {
                return;
            }

            fakeGiftTarget = this.getLatestDayGameInfo().getExecutedAgent();
            if(Math.random() < 0.3D) {
                fakeResult = Species.WEREWOLF;
            } else {
                fakeResult = Species.HUMAN;
            }
        }

        if(fakeGiftTarget != null) {
            this.fakeJudgeList.add(new Judge(this.getDay(), this.getMe(), fakeGiftTarget, fakeResult));
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
