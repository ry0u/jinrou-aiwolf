package org.aiwolf.Kog;

import org.aiwolf.client.base.player.AbstractVillager;
import org.aiwolf.client.lib.TemplateTalkFactory;
import org.aiwolf.client.lib.Topic;
import org.aiwolf.client.lib.Utterance;
import org.aiwolf.common.data.*;
import org.aiwolf.common.net.GameInfo;

import java.util.*;

/**
 * Created by ry0u on 15/08/10.
 */
public class KogVillagerPlayer extends AbstractVillager {
    Agent planningVoteAgent;
    Agent declaredPlanningVoteAgent;
    List<Agent> priorityAgents = new ArrayList<>();
    List<Agent> seerPlayers = new ArrayList<>();

    Map<Agent, Role> comingoutRole = new HashMap<>();
    Map<Agent, Utterance> allTalkList = new HashMap<>();
    List<Judge> judgeList = new ArrayList<>();

    boolean overSeer = false;
    boolean overMedium = false;

    int firstSeerDay = 0;
    int readTalkListNum;

    public KogVillagerPlayer() {
    }

    @Override
    public void dayStart() {
        this.declaredPlanningVoteAgent = null;
        this.planningVoteAgent = null;

        this.setPlanningVoteAgent();
        this.readTalkListNum = 0;

        List<Agent> agents = getLatestDayGameInfo().getAliveAgentList();
        List<Agent> newPriority = new ArrayList<>();
        for(int i=0;i<this.priorityAgents.size();i++) {
            Agent priority = this.priorityAgents.get(i);
            if(agents.contains(priority)) {
                newPriority.add(priority);
            }
        }

        this.priorityAgents = newPriority;
    }

    @Override
    public String talk() {
        if(this.declaredPlanningVoteAgent != this.planningVoteAgent) {
            String ret = TemplateTalkFactory.vote(this.planningVoteAgent);
            this.declaredPlanningVoteAgent = this.planningVoteAgent;
            return ret;
        } else {
            return TemplateTalkFactory.over();
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

        for(int i= this.readTalkListNum;i < talkList.size();i++) {
            Talk talk = (Talk)talkList.get(i);
            Utterance utterance = new Utterance(talk.getContent());

            this.allTalkList.put(talk.getAgent(), utterance);

            if(utterance.getTopic().equals(Topic.COMINGOUT)) {
                if(utterance.getRole().equals(Role.SEER)) {
                    this.seerPlayers.add(utterance.getTarget());

                    if(this.firstSeerDay == 0 || this.firstSeerDay == getLatestDayGameInfo().getDay()) {
                        this.firstSeerDay = getLatestDayGameInfo().getDay();
                        this.comingoutRole.put(utterance.getTarget(), utterance.getRole());
                    } else {
                        this.priorityAgents.add(utterance.getTarget());
                    }
                } else {
                    comingoutRole.put(utterance.getTarget(), utterance.getRole());
                }
            }

            if(utterance.getTopic().equals(Topic.DIVINED)) {
                Judge judge = new Judge(this.getLatestDayGameInfo().getDay(), talk.getAgent(), utterance.getTarget(), utterance.getResult());
                this.judgeList.add(judge);
                existInspectResult = true;
            }

        }

        this.readTalkListNum = talkList.size();
        if(existInspectResult) {
            this.setPlanningVoteAgent();
        }

        Map<Agent,Integer> judgeCnt = new HashMap<>();
        for(int i=0;i<judgeList.size();i++) {
            Judge judge = judgeList.get(i);
            if(judge.getResult().equals(Species.HUMAN)) {
                if(judgeCnt.containsKey(judge.getTarget())) {
                    int cnt = judgeCnt.get(judge.getTarget());
                    judgeCnt.replace(judge.getTarget(),cnt+1);
                } else {
                    judgeCnt.put(judge.getTarget(),0);
                }
            }
        }

        List<Agent> whiteAgent = new ArrayList<>();
        for(Agent agent : judgeCnt.keySet()) {
            int cnt = judgeCnt.get(agent);
            if(cnt == seerPlayers.size()) {
                whiteAgent.add(agent);
            }
        }

        List<Agent> aliveAgentList = this.getLatestDayGameInfo().getAliveAgentList();
        for(int i=0;i<whiteAgent.size();i++) {
            aliveAgentList.remove(whiteAgent.get(i));
        }

        if(aliveAgentList.size() > 0) {
            Random rand = new Random();
            int id = rand.nextInt(aliveAgentList.size());
            this.planningVoteAgent = aliveAgentList.get(id);
        }
    }

    public void setPlanningVoteAgent() {
        if(priorityAgents.size() > 0) {
            this.planningVoteAgent = randomSelect(priorityAgents);
            return;
        }

        if(this.planningVoteAgent != null) {
            Iterator ite = this.judgeList.iterator();

            while(ite.hasNext()) {
                Judge judge = (Judge)ite.next();
                if(judge.getTarget().equals(this.planningVoteAgent)) {
                    return;
                }
            }
        }

        ArrayList voteAgentCandidate = new ArrayList();
        List<Agent> aliveAgentList = this.getLatestDayGameInfo().getAliveAgentList();
        aliveAgentList.remove(this.getMe());
        Iterator ite = this.judgeList.iterator();

        if(this.judgeList.size() > 0) {
            while (ite.hasNext()) {
                Judge judge = (Judge) ite.next();
                if (aliveAgentList.contains(judge.getTarget()) && judge.getResult() == Species.WEREWOLF) {
                    voteAgentCandidate.add(judge.getTarget());
                }
            }

            Random random = new Random();
            if (voteAgentCandidate.size() > 0) {
                int id = random.nextInt(voteAgentCandidate.size());
                this.planningVoteAgent = (Agent) voteAgentCandidate.get(id);
            } else {
                int id = random.nextInt(aliveAgentList.size());
                this.planningVoteAgent = aliveAgentList.get(id);
            }
        } else {
            List<Agent> voteSeer = new ArrayList();
            List<Agent> voteMedium = new ArrayList();

            Iterator ite2 = aliveAgentList.iterator();
            while(ite2.hasNext()) {
                Agent agent = (Agent)ite2.next();
                if(this.comingoutRole.containsKey(agent)) {
                    if(this.comingoutRole.get(agent).equals(Role.SEER)) {
                        voteSeer.add(agent);
                    }
                    if(this.comingoutRole.get(agent).equals(Role.MEDIUM)) {
                        voteMedium.add(agent);
                    }
                }
            }

            if(voteSeer.size() >= 3) {
                this.overSeer = true;
            }

            if(overSeer) {
                this.planningVoteAgent = randomSelect(voteSeer);
            }

            if(voteMedium.size() >= 2) {
                this.overMedium = true;
            }

            if(overMedium) {
                this.planningVoteAgent = randomSelect(voteMedium);
            }

            if(!overSeer && !overMedium) {
                for(int i=0;i<voteSeer.size();i++) {
                    Agent agent = voteSeer.get(i);
                    if(aliveAgentList.contains(agent)) {
                        aliveAgentList.remove(agent);
                    }
                }

                for(int i=0;i<voteMedium.size();i++) {
                    Agent agent = voteMedium.get(i);
                    if(aliveAgentList.contains(agent)) {
                        aliveAgentList.remove(agent);
                    }
                }

                this.planningVoteAgent = randomSelect(aliveAgentList);
            }

            if(voteSeer.size() <= 1) {
                this.overSeer = false;
            }

            if(voteMedium.size() <= 1) {
                this.overMedium = false;
            }


        }
    }

    public Agent randomSelect(List<Agent> agents) {
        Random rand = new Random();
        int id = rand.nextInt(agents.size());
        return agents.get(id);
    }
}
