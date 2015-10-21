package org.aiwolf.Kog;

import org.aiwolf.client.base.player.AbstractSeer;
import org.aiwolf.client.lib.TemplateTalkFactory;
import org.aiwolf.client.lib.Topic;
import org.aiwolf.client.lib.Utterance;
import org.aiwolf.common.data.*;
import org.aiwolf.common.net.GameInfo;
import org.aiwolf.common.net.GameSetting;

import java.util.*;

/**
 * Created by ry0u on 15/08/07.
 */
public class KogSeerPlayer extends AbstractSeer {

    Judge judge = null;
    List<Judge> myJudgeList = new ArrayList<>();
    List<Judge> anotherJudgeList = new ArrayList<>();
    HashMap<Agent, Role> comingoutRole = new HashMap<>();

    boolean isComingout = false;
    boolean isAgainstSeer = false;
    boolean tellMyJudge = false;

    private int readTalkNum = 0;

    public KogSeerPlayer() {

    }

    @Override
    public void initialize(GameInfo gameInfo, GameSetting gameSetting) {
        super.initialize(gameInfo, gameSetting);

        this.isComingout = false;
        this.isAgainstSeer = false;
    }

    @Override
    public void dayStart() {
        super.dayStart();

        this.tellMyJudge = false;
        this.readTalkNum = 0;

        if (getLatestDayGameInfo().getDivineResult() != null) {
            judge = getLatestDayGameInfo().getDivineResult();
            myJudgeList.add(judge);
        }

        HashMap<Agent, Role> newRole = new HashMap<>();
        List<Agent> aliveAgentList = this.getLatestDayGameInfo().getAliveAgentList();

        for(Agent agent : comingoutRole.keySet()) {
            if(aliveAgentList.contains(agent)) {
                newRole.put(agent,this.comingoutRole.get(agent));
            }
        }

        this.comingoutRole = newRole;
    }

    @Override
    public String talk() {
        if(!isComingout) {
            isComingout = true;
            String ret = TemplateTalkFactory.comingout(getMe(), getMyRole());
            return ret;
        }

        if(!tellMyJudge) {
            tellMyJudge = true;
            Judge judge = this.getLatestDayGameInfo().getDivineResult();
            if(judge != null) {
                String ret = TemplateTalkFactory.divined(judge.getTarget(), judge.getResult());
                return ret;
            }
        }

        return Talk.OVER;
    }

    @Override
    public Agent vote() {

        List<Agent> whiteAgent = new ArrayList<>();
        List<Agent> blackAgent = new ArrayList<>();

        for(Judge j : getMyJudgeList()) {
            if(getLatestDayGameInfo().getAliveAgentList().contains(judge.getTarget())) {
                switch (j.getResult()) {
                    case HUMAN:
                        whiteAgent.add(judge.getTarget());
                        break;
                    case WEREWOLF:
                        blackAgent.add(judge.getTarget());
                        break;
                }
            }
        }

        if(blackAgent.size() > 0) {
            return randomSelect(blackAgent);
        } else {
            List<Agent> voteCandidates = new ArrayList<Agent>();
            voteCandidates.addAll(getLatestDayGameInfo().getAliveAgentList());
            voteCandidates.remove(getMe());
            voteCandidates.removeAll(whiteAgent);

            return randomSelect(voteCandidates);
        }
    }

    @Override
    public Agent divine() {
        List<Agent> aliveAgentList = getLatestDayGameInfo().getAliveAgentList();
        aliveAgentList.remove(this.getMe());

        if(!isAgainstSeer) {
            return randomSelect(aliveAgentList);
        } else {
            double flag = Math.random();
            if(0.00D <= flag && flag < 0.05D) {
                List<Agent> againstSeer = new ArrayList<>();
                for(Agent agent : this.comingoutRole.keySet()) {
                    Role role = this.comingoutRole.get(agent);
                    if(role.equals(Role.SEER) && !agent.equals(this.getMe())) {
                        againstSeer.add(agent);
                    }
                }

                if(againstSeer.size() > 0) {
                    return randomSelect(againstSeer);
                }
            } else if(0.05 <= flag && flag < 0.75D) {
                List<Agent> anotherDivinedAgent = new ArrayList();
                for(int i=0; i<this.anotherJudgeList.size(); i++) {
                    Judge judge = anotherJudgeList.get(i);
                    if(!anotherDivinedAgent.contains(judge.getTarget())) {
                        anotherDivinedAgent.add(judge.getTarget());
                    }
                }

                if(anotherDivinedAgent.size() > 0) {
                    return randomSelect(anotherDivinedAgent);
                }
            }

            return randomSelect(aliveAgentList);
        }
    }

    @Override
    public void update(GameInfo gameInfo) {
        super.update(gameInfo);

        List<Talk> talkList = gameInfo.getTalkList();

        for(int i=readTalkNum;i<talkList.size();i++) {
            Talk talk = talkList.get(i);
            Utterance utterance = new Utterance(talk.getContent());

            if(utterance.getTopic().equals(Topic.COMINGOUT)) {
                this.comingoutRole.put(utterance.getTarget(),utterance.getRole());
                if(utterance.getRole().equals(Role.SEER)) {
                    this.isAgainstSeer = true;
                }
            }

            if(utterance.getTopic().equals(Topic.DIVINED)) {
                if(!talk.getAgent().equals(this.getMe())) {
                    Judge judge = new Judge(getLatestDayGameInfo().getDay(), talk.getAgent(), utterance.getTarget(), utterance.getResult());
                    this.anotherJudgeList.add(judge);
                }
            }
        }
    }

    @Override
    public void finish() {

    }

    public Agent randomSelect(List<Agent> list)  {
        Random rand = new Random();
        int d = rand.nextInt(list.size());

        List<Agent> ret = getLatestDayGameInfo().getAliveAgentList();
        return ret.get(d);
    }
}
