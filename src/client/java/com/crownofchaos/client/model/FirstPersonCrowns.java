package com.crownofchaos.client.model;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Optional;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;

/**
 * Decides when a worn crown should be skipped because the wearer is being drawn as their own
 * first-person body. Vanilla never draws your own player in first person, but mods like Better
 * Combat do (through PlayerAnimationLib) to show your arms swinging; they hide vanilla helmets
 * then, and the crowns, which have their own renderers, must hide too or they cover the screen.
 *
 * <p>Only that first-person pass is affected: other players, your own third-person view and the
 * inventory preview are separate renders and still show the crown.
 */
final class FirstPersonCrowns {
	/** PlayerAnimationLib's flag on the player's render state, added by its mixin. */
	private static final String PLAYER_ANIM_FIRST_PERSON = "playerAnimLib$isFirstPersonPass";
	/** The flag's method per render state class, or empty where the class doesn't have it. */
	private static final Map<Class<?>, Optional<Method>> FLAG_METHODS = new ConcurrentHashMap<>();

	private FirstPersonCrowns() {
	}

	/** Whether to leave the crown off this time the wearer is drawn. */
	static boolean hide(HumanoidRenderState state, HumanoidModel<?> wearerModel) {
		// Mods that draw a first-person body generally hide its head; no head, no crown.
		if (!wearerModel.head.visible) {
			return true;
		}
		return FLAG_METHODS.computeIfAbsent(state.getClass(), FirstPersonCrowns::findFlag)
				.map(method -> isSet(method, state))
				.orElse(false);
	}

	private static Optional<Method> findFlag(Class<?> stateClass) {
		try {
			Method method = stateClass.getMethod(PLAYER_ANIM_FIRST_PERSON);
			return method.getReturnType() == boolean.class ? Optional.of(method) : Optional.empty();
		} catch (NoSuchMethodException | SecurityException e) {
			return Optional.empty();
		}
	}

	private static boolean isSet(Method method, HumanoidRenderState state) {
		try {
			return (boolean) method.invoke(state);
		} catch (ReflectiveOperationException | RuntimeException e) {
			return false;
		}
	}
}
