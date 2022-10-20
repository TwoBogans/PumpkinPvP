package org.au2b2t.pumpkinpvp;

import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByBlockEvent;

public class PumpkinDamageEvent extends EntityDamageByBlockEvent {

    private final Player damagee;

    private final Player pumpkinPlacer;

    public PumpkinDamageEvent(Block damager, Player damagee, Player pumpkinPlacer) {
        super(damager, damagee, DamageCause.BLOCK_EXPLOSION, damagee.getLastDamage());
        this.pumpkinPlacer = pumpkinPlacer;
        this.damagee = damagee;
    }

    public Player getDamagee() {
        return damagee;
    }

    public Player getPumpkinPlacer() {
        return pumpkinPlacer;
    }
}
