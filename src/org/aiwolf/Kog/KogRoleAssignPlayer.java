package org.aiwolf.Kog;

import org.aiwolf.client.base.player.AbstractRoleAssignPlayer;

/**
 * Created by ry0u on 15/08/06.
 */

public class KogRoleAssignPlayer extends AbstractRoleAssignPlayer{

    public KogRoleAssignPlayer() {
        this.setVillagerPlayer(new KogVillagerPlayer());
        this.setSeerPlayer(new KogSeerPlayer());
        this.setMediumPlayer(new KogMediumPlayer());
        this.setBodyguardPlayer(new KogBodyGuardPlayer());
        this.setPossessedPlayer(new KogPossessedPlayer());
        this.setWerewolfPlayer(new KogWolfPlayer());
    }

    @Override
    public String getName() {
        return "KogPlayer";
    }

}
