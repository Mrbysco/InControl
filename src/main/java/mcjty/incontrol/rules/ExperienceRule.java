package mcjty.incontrol.rules;

import com.google.gson.JsonElement;
import mcjty.incontrol.ErrorHandler;
import mcjty.incontrol.InControl;
import mcjty.incontrol.compat.ModRuleCompatibilityLayer;
import mcjty.incontrol.data.PhaseTools;
import mcjty.incontrol.rules.support.GenericRuleEvaluator;
import mcjty.incontrol.tools.rules.IEventQuery;
import mcjty.incontrol.tools.rules.IModRuleCompatibilityLayer;
import mcjty.incontrol.tools.rules.RuleBase;
import mcjty.incontrol.tools.typed.Attribute;
import mcjty.incontrol.tools.typed.AttributeMap;
import mcjty.incontrol.tools.typed.GenericAttributeMapFactory;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.DamageSource;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.event.entity.living.LivingExperienceDropEvent;
import net.minecraftforge.eventbus.api.Event;

import java.util.Set;

import static mcjty.incontrol.rules.support.RuleKeys.*;

public class ExperienceRule extends RuleBase<RuleBase.EventGetter> {

    public static final IEventQuery<LivingExperienceDropEvent> EVENT_QUERY = new IEventQuery<LivingExperienceDropEvent>() {
        @Override
        public World getWorld(LivingExperienceDropEvent o) {
            return o.getEntity().getCommandSenderWorld();
        }

        @Override
        public BlockPos getPos(LivingExperienceDropEvent o) {
            return o.getEntity().blockPosition();
        }

        @Override
        public BlockPos getValidBlockPos(LivingExperienceDropEvent o) {
            return o.getEntity().blockPosition().below();
        }

        @Override
        public int getY(LivingExperienceDropEvent o) {
            return o.getEntity().blockPosition().getY();
        }

        @Override
        public Entity getEntity(LivingExperienceDropEvent o) {
            return o.getEntity();
        }

        @Override
        public DamageSource getSource(LivingExperienceDropEvent o) {
            return null;
        }

        @Override
        public Entity getAttacker(LivingExperienceDropEvent o) {
            return o.getAttackingPlayer();
        }

        @Override
        public PlayerEntity getPlayer(LivingExperienceDropEvent o) {
            return o.getAttackingPlayer();
        }

        @Override
        public ItemStack getItem(LivingExperienceDropEvent o) {
            return ItemStack.EMPTY;
        }
    };
    private static final GenericAttributeMapFactory FACTORY = new GenericAttributeMapFactory();

    static {
        FACTORY
                .attribute(Attribute.create(MINTIME))
                .attribute(Attribute.create(MAXTIME))
                .attribute(Attribute.create(MINLIGHT))
                .attribute(Attribute.create(MAXLIGHT))
                .attribute(Attribute.create(MINHEIGHT))
                .attribute(Attribute.create(MAXHEIGHT))
                .attribute(Attribute.create(MINDIFFICULTY))
                .attribute(Attribute.create(MAXDIFFICULTY))
                .attribute(Attribute.create(MINSPAWNDIST))
                .attribute(Attribute.create(MAXSPAWNDIST))
                .attribute(Attribute.create(RANDOM))
                .attribute(Attribute.create(INBUILDING))
                .attribute(Attribute.create(INCITY))
                .attribute(Attribute.create(INSTREET))
                .attribute(Attribute.create(INSPHERE))
                .attribute(Attribute.create(PASSIVE))
                .attribute(Attribute.create(HOSTILE))
                .attribute(Attribute.create(SEESKY))
                .attribute(Attribute.create(WEATHER))
                .attribute(Attribute.createMulti(CATEGORY))
                .attribute(Attribute.create(DIFFICULTY))
                .attribute(Attribute.create(STRUCTURE))
                .attribute(Attribute.create(PLAYER))
                .attribute(Attribute.create(REALPLAYER))
                .attribute(Attribute.create(FAKEPLAYER))
                .attribute(Attribute.create(WINTER))
                .attribute(Attribute.create(SUMMER))
                .attribute(Attribute.create(SPRING))
                .attribute(Attribute.create(AUTUMN))
                .attribute(Attribute.create(STATE))
                .attribute(Attribute.create(PSTATE))
                .attribute(Attribute.createMulti(MOB))
                .attribute(Attribute.createMulti(MOD))
                .attribute(Attribute.createMulti(BLOCK))
                .attribute(Attribute.create(BLOCKOFFSET))
                .attribute(Attribute.createMulti(BIOME))
                .attribute(Attribute.createMulti(BIOMETYPE))
                .attribute(Attribute.createMulti(DIMENSION))
                .attribute(Attribute.createMulti(DIMENSION_MOD))
                .attribute(Attribute.createMulti(HELDITEM))
                .attribute(Attribute.createMulti(PLAYER_HELDITEM))
                .attribute(Attribute.createMulti(OFFHANDITEM))
                .attribute(Attribute.createMulti(BOTHHANDSITEM))

                .attribute(Attribute.create(ACTION_RESULT))
                .attribute(Attribute.create(ACTION_SETXP))
                .attribute(Attribute.create(ACTION_ADDXP))
                .attribute(Attribute.create(ACTION_MULTXP))
        ;
    }

    private final GenericRuleEvaluator ruleEvaluator;
    private final Set<String> phases;
    private Event.Result result;
    private Integer xp = null;
    private float multxp = 1.0f;
    private float addxp = 0.0f;

    private ExperienceRule(AttributeMap map, Set<String> phases) {
        super(InControl.setup.getLogger());
        this.phases = phases;
        ruleEvaluator = new GenericRuleEvaluator(map);
        addActions(map, new ModRuleCompatibilityLayer());
    }

    public Set<String> getPhases() {
        return phases;
    }

    public static ExperienceRule parse(JsonElement element) {
        if (element == null) {
            return null;
        } else {
            AttributeMap map = FACTORY.parse(element, "experience.json");
            return new ExperienceRule(map, PhaseTools.getPhases(element));
        }
    }

    public int modifyXp(int xpIn) {
        if (xp != null) {
            xpIn = xp;
        }
        return (int) (xpIn * multxp + addxp);
    }

    @Override
    protected void addActions(AttributeMap map, IModRuleCompatibilityLayer layer) {
        super.addActions(map, layer);

        map.consumeOrElse(ACTION_RESULT, br -> {
            if ("default".equals(br) || br.startsWith("def")) {
                this.result = Event.Result.DEFAULT;
            } else if ("allow".equals(br) || "true".equals(br)) {
                this.result = Event.Result.ALLOW;
            } else {
                this.result = Event.Result.DENY;
            }
        }, () -> {
            this.result = Event.Result.DEFAULT;
        });

        map.consume(ACTION_SETXP, v -> xp = v);
        map.consume(ACTION_ADDXP, v -> addxp = v);
        map.consume(ACTION_MULTXP, v -> multxp = v);

        if (!map.isEmpty()) {
            StringBuffer buffer = new StringBuffer();
            map.getKeys().forEach(k -> buffer.append(k).append(' '));
            ErrorHandler.error("Invalid keywords in experience rule: " + buffer);
        }
    }

    public boolean match(LivingExperienceDropEvent event) {
        return ruleEvaluator.match(event, EVENT_QUERY);
    }

    public Event.Result getResult() {
        return result;
    }
}
