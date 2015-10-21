package org.aiwolf.Kog;

import org.aiwolf.client.base.player.AbstractMedium;
import org.aiwolf.client.lib.TemplateTalkFactory;
import org.aiwolf.client.lib.Topic;
import org.aiwolf.client.lib.Utterance;
import org.aiwolf.common.data.*;
import org.aiwolf.common.net.GameInfo;
import org.aiwolf.common.net.GameSetting;

import java.util.*;

/**
 * Created by ry0u on 15/08/13.
 */
public class KogMediumPlayer extends AbstractMedium {

    int readTalkListNum;
    int cominguoutDay;
    boolean isComingout;

    ArrayList<Judge> declaredJudgeAgentList = new ArrayList();
    ArrayList<Judge> judgeList = new ArrayList<>();
    HashMap<Agent, Role> comingoutRole = new HashMap<>();

    boolean isSaidAllInquestResult;

    Agent planningVoteAgent;
    Agent declaredPlanningVoteAgent;

    public KogMediumPlayer() {
    }

    @Override
    public void initialize(GameInfo gameInfo, GameSetting gameSetting){
        super.initialize(gameInfo, gameSetting);
        Random rand = new Random();
        this.cominguoutDay = rand.nextInt(3) + 1;
        this.isComingout = false;
    }

    @Override
    public void dayStart() {
        super.dayStart();
        this.declaredPlanningVoteAgent = null;
        this.planningVoteAgent = null;
        this.setPlanningVoteAgent();
        this.isSaidAllInquestResult = false;
        this.readTalkListNum = 0;
    }

    @Override
    public String talk() {
        if(!this.isComingout && this.getDay() >= this.cominguoutDay) {
            String ret = TemplateTalkFactory.comingout(this.getMe(),this.getMyRole());
            this.isComingout = true;
            return ret;
        } else {
            if(this.isComingout && !this.isSaidAllInquestResult) {
                Iterator ite = this.getMyJudgeList().iterator();

                while(ite.hasNext()) {
                    Judge judge = (Judge)ite.next();
                    if(!this.declaredJudgeAgentList.contains(judge)) {
                        String ret = TemplateTalkFactory.inquested(judge.getTarget(),judge.getResult());
                        this.declaredJudgeAgentList.add(judge);
                        return ret;
                    }
                }

                this.isSaidAllInquestResult = true;
            }

            if(this.declaredPlanningVoteAgent != this.planningVoteAgent) {
                String ret = TemplateTalkFactory.vote(this.planningVoteAgent);
                this.declaredPlanningVoteAgent = this.planningVoteAgent;
                return ret;
            } else {
                return TemplateTalkFactory.over();
            }
        }
    }

    @Override
    public Agent vote() {
        return this.planningVoteAgent;
    }

    @Override
    public void finish() {
    }


    @Override
    public void update(GameInfo gameInfo) {
        super.update(gameInfo);
        List talkList = gameInfo.getTalkList();
        boolean existInspectResult = false;

        for(int i=this.readTalkListNum; i < talkList.size(); i++) {
            Talk talk = (Talk)talkList.get(i);
            Utterance utterance = new Utterance(talk.getContent());

            if(utterance.getTopic().equals(Topic.COMINGOUT)) {
                this.comingoutRole.put(talk.getAgent(),utterance.getRole());
                if(utterance.getRole().equals(this.getMyRole())) {
                    this.setPlanningVoteAgent();
                }
            }
            else if(utterance.getTopic().equals(Topic.DIVINED)) {
                Judge judge = new Judge(this.getDay(),talk.getAgent(),utterance.getTarget(),utterance.getResult());
                this.judgeList.add(judge);
                existInspectResult = true;
            }

        }

        this.readTalkListNum = talkList.size();
        if(existInspectResult) {
            this.setPlanningVoteAgent();
        }
    }

    public void setPlanningVoteAgent() {
        ArrayList voteAgentCandidate = new ArrayList();
        List aliveAgentList = this.getLatestDayGameInfo().getAliveAgentList();
        aliveAgentList.remove(this.getMe());
        Iterator rand = aliveAgentList.iterator();

        while (rand.hasNext()) {
            Agent subVoteAgentCandidate = (Agent) rand.next();
            if (this.comingoutRole.containsKey(subVoteAgentCandidate) && this.comingoutRole.get(subVoteAgentCandidate) == Role.MEDIUM) {
                voteAgentCandidate.add(subVoteAgentCandidate);
            }
        }

        rand = this.getMyJudgeList().iterator();

        while (rand.hasNext()) {
            Judge subVoteAgentCandidate1 = (Judge) rand.next();
            Iterator var6 = this.judgeList.iterator();

            while (var6.hasNext()) {
                Judge otherJudge = (Judge) var6.next();
                if (aliveAgentList.contains(otherJudge.getAgent()) && subVoteAgentCandidate1.getTarget().equals(otherJudge.getTarget()) && subVoteAgentCandidate1.getResult() != otherJudge.getResult()) {
                    voteAgentCandidate.add(otherJudge.getAgent());
                }
            }
        }

        if (this.planningVoteAgent == null && !voteAgentCandidate.contains(this.planningVoteAgent)) {
            if (voteAgentCandidate.size() > 0) {
                Random subVoteAgentCandidate = new Random();
                this.planningVoteAgent = (Agent) voteAgentCandidate.get(subVoteAgentCandidate.nextInt(voteAgentCandidate.size()));
            } else {
                ArrayList subVoteAgentCandidate3 = new ArrayList();
                Iterator otherJudge1 = this.judgeList.iterator();

                while (otherJudge1.hasNext()) {
                    Judge rand1 = (Judge) otherJudge1.next();
                    if (aliveAgentList.contains(rand1.getTarget()) && rand1.getResult() == Species.WEREWOLF) {
                        subVoteAgentCandidate3.add(rand1.getTarget());
                    }

                }

                Random rand2 = new Random();
                if (subVoteAgentCandidate3.size() > 0) {
                    int id = rand2.nextInt(subVoteAgentCandidate3.size());
                    this.planningVoteAgent = (Agent) subVoteAgentCandidate3.get(id);
                } else {
                    int id = rand2.nextInt(aliveAgentList.size());
                    this.planningVoteAgent = (Agent) aliveAgentList.get(id);
                }
            }
        }
    }




}
