package org.aiwolf.Kog;

import org.aiwolf.client.base.player.AbstractBodyguard;
import org.aiwolf.client.lib.TemplateTalkFactory;
import org.aiwolf.client.lib.Topic;
import org.aiwolf.client.lib.Utterance;
import org.aiwolf.common.data.*;
import org.aiwolf.common.net.GameInfo;

import java.util.*;

/**
 * Created by ry0u on 15/08/13.
 */

public class KogBodyGuardPlayer extends AbstractBodyguard {
    Agent planningVoteAgent;
    Agent declaredPlanningVoteAgent;

    int readTalkListNum;

    HashMap<Agent,Role> comigoutRole = new HashMap<>();
    List<Judge> judgeList = new ArrayList<>();

    public KogBodyGuardPlayer() {
    }

    @Override
    public void dayStart() {
        this.declaredPlanningVoteAgent = null;
        this.planningVoteAgent = null;
        this.setPlanningVoteAgent();
        this.readTalkListNum = 0;
    }

    @Override
    public String talk() {
        if(this.declaredPlanningVoteAgent != this.planningVoteAgent) {
            String string = TemplateTalkFactory.vote(this.planningVoteAgent);
            this.declaredPlanningVoteAgent = this.planningVoteAgent;
            return string;
        } else {
            return TemplateTalkFactory.over();
        }
    }

    @Override
    public Agent vote() {
        return this.planningVoteAgent;
    }

    @Override
    public Agent guard() {

        List<Agent> guardSeer = new ArrayList();
        List<Agent> guardMedium = new ArrayList();
        List<Agent> aliveAgentList = this.getLatestDayGameInfo().getAliveAgentList();
        aliveAgentList.remove(this.getMe());
        Iterator it = aliveAgentList.iterator();

        if(Math.random() <= 0.05D) {
            Random rand = new Random();
            int id = rand.nextInt(aliveAgentList.size());
            return aliveAgentList.get(id);
        } else {

            Agent guardAgent;

            while (it.hasNext()) {
                guardAgent = (Agent) it.next();
                if (this.comigoutRole.containsKey(guardAgent) && this.comigoutRole.get(guardAgent).equals(Role.SEER)) {
                    guardSeer.add(guardAgent);
                }

                if (this.comigoutRole.containsKey(guardAgent) && this.comigoutRole.get(guardAgent).equals(Role.MEDIUM)) {
                    guardMedium.add(guardAgent);
                }
            }

            Random rand = new Random();
            if (guardSeer.size() == 0 && guardMedium.size() > 0) {
                int id = rand.nextInt(guardMedium.size());
                guardAgent = guardMedium.get(id);
            } else if (guardSeer.size() >= 3) {
                Iterator ite = guardSeer.iterator();
                while (ite.hasNext()) {
                    Agent del = (Agent) ite.next();
                    aliveAgentList.remove(del);
                }

                if (aliveAgentList.size() == 0) {
                    return null;
                } else {
                    int id = rand.nextInt(aliveAgentList.size());
                    guardAgent = aliveAgentList.get(id);
                }
            } else if (guardSeer.size() >= 1) {
                int id = rand.nextInt(guardSeer.size());
                guardAgent = guardSeer.get(id);
            } else {
                int id = rand.nextInt(aliveAgentList.size());
                guardAgent = aliveAgentList.get(id);
            }

            return guardAgent;
        }
    }

    @Override
    public void finish() {
    }

    @Override
    public void update(GameInfo gameInfo) {
        super.update(gameInfo);
        List talkList = gameInfo.getTalkList();
        boolean existInspectResult = false;

        for(int i = this.readTalkListNum; i < talkList.size(); ++i) {
            Talk talk = (Talk)talkList.get(i);
            Utterance utterance = new Utterance(talk.getContent());

            if(utterance.getTopic().equals(Topic.COMINGOUT)) {
                this.comigoutRole.put(utterance.getTarget(),utterance.getRole());
            }

            if(utterance.getTopic().equals(Topic.DIVINED)) {
                Judge judge = new Judge(this.getDay(), talk.getAgent(), utterance.getTarget(), utterance.getResult());
                judgeList.add(judge);
                existInspectResult = true;
            }
        }

        this.readTalkListNum = talkList.size();
        if(existInspectResult) {
            this.setPlanningVoteAgent();
        }

    }

    public void setPlanningVoteAgent() {
        if(this.planningVoteAgent != null) {
            Iterator aliveAgentList = this.judgeList.iterator();

            while(aliveAgentList.hasNext()) {
                Judge voteAgentCandidate = (Judge)aliveAgentList.next();
                if(voteAgentCandidate.getTarget().equals(this.planningVoteAgent)) {
                    return;
                }
            }
        }

        List<Agent> voteAgentCandidate = new ArrayList();
        List<Agent> aliveAgentList = this.getLatestDayGameInfo().getAliveAgentList();

        aliveAgentList.remove(this.getMe());
        Iterator var4 = this.judgeList.iterator();

        while(var4.hasNext()) {
            Judge judge = (Judge)var4.next();
            if(aliveAgentList.contains(judge.getTarget()) && judge.getResult() == Species.WEREWOLF) {
                voteAgentCandidate.add(judge.getTarget());
            }

            if(aliveAgentList.contains(judge.getTarget()) && judge.getResult() == Species.HUMAN) {
                aliveAgentList.remove(judge.getTarget());
            }
        }

        Random rand = new Random();
        if(voteAgentCandidate.size() > 0) {
            int id = rand.nextInt(voteAgentCandidate.size());
            this.planningVoteAgent = voteAgentCandidate.get(id);
        } else {
            int id = rand.nextInt(aliveAgentList.size());
            this.planningVoteAgent = aliveAgentList.get(id);
        }
    }

}
