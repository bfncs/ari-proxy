package io.retel.ariproxy.boundary.commandsandresponses.auxiliary;

import static io.retel.ariproxy.boundary.commandsandresponses.auxiliary.Companion.RESOURCE_ID_POSITION;
import static io.retel.ariproxy.boundary.commandsandresponses.auxiliary.Companion.RESOURCE_ID_POSITION_ON_ANOTHER_RESOURCE;
import static io.vavr.API.None;
import static io.vavr.API.Some;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import io.vavr.Value;
import io.vavr.collection.List;
import io.vavr.control.Option;
import io.vavr.control.Try;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import org.apache.commons.lang3.StringUtils;

public enum AriCommandType {
	BRIDGE_CREATION(
			true,
			resourceIdFromUri(RESOURCE_ID_POSITION),
			resourceIdFromBody("/bridgeId")
	),
	BRIDGE(
			false,
			resourceIdFromUri(RESOURCE_ID_POSITION),
			resourceIdFromBody("/bridgeId")
	),
	CHANNEL_CREATION(
			true,
			resourceIdFromUri(RESOURCE_ID_POSITION),
			resourceIdFromBody("/channelId")
	),
	CHANNEL(
			false,
			resourceIdFromUri(RESOURCE_ID_POSITION),
			resourceIdFromBody("/channelId")
	),
	PLAYBACK_CREATION(
			true,
			resourceIdFromUri(RESOURCE_ID_POSITION_ON_ANOTHER_RESOURCE),
			resourceIdFromBody("/playbackId")
	),
	PLAYBACK(
			false,
			resourceIdFromUri(RESOURCE_ID_POSITION_ON_ANOTHER_RESOURCE),
			resourceIdFromBody("/playbackId")
	),
	RECORDING_CREATION(
			true,
			AriCommandType::notAvailable,
			resourceIdFromBody("/name")
	),
	RECORDING(false, AriCommandType::notAvailable, resourceIdFromBody("/name")),
	SNOOPING_CREATION(
			true,
			resourceIdFromUri(RESOURCE_ID_POSITION_ON_ANOTHER_RESOURCE),
			resourceIdFromBody("/snoopId")
	),
	SNOOPING(
			false,
			resourceIdFromUri(RESOURCE_ID_POSITION_ON_ANOTHER_RESOURCE),
			resourceIdFromBody("/snoopId")
	),
	UNKNOWN(false, uri -> None(), body -> None());

	private static final Map<String, AriCommandType> pathsToCommandTypes = new HashMap<>();
	static {
		pathsToCommandTypes.put("/channels", CHANNEL_CREATION);
		pathsToCommandTypes.put("/channels/create", CHANNEL_CREATION);
		pathsToCommandTypes.put("/channels/{channelId}", CHANNEL_CREATION);
		pathsToCommandTypes.put("/channels/{channelId}/continue", CHANNEL);
		pathsToCommandTypes.put("/channels/{channelId}/move", CHANNEL);
		pathsToCommandTypes.put("/channels/{channelId}/redirect", CHANNEL);
		pathsToCommandTypes.put("/channels/{channelId}/answer", CHANNEL);
		pathsToCommandTypes.put("/channels/{channelId}/ring", CHANNEL);
		pathsToCommandTypes.put("/channels/{channelId}/dtmf", CHANNEL);
		pathsToCommandTypes.put("/channels/{channelId}/mute", CHANNEL);
		pathsToCommandTypes.put("/channels/{channelId}/hold", CHANNEL);
		pathsToCommandTypes.put("/channels/{channelId}/moh", CHANNEL);
		pathsToCommandTypes.put("/channels/{channelId}/silence", CHANNEL);
		pathsToCommandTypes.put("/channels/{channelId}/play", PLAYBACK_CREATION);
		pathsToCommandTypes.put(
				"/channels/{channelId}/play/{playbackId}",
				PLAYBACK_CREATION
		);
		pathsToCommandTypes.put("/channels/{channelId}/record", RECORDING_CREATION);
		pathsToCommandTypes.put("/channels/{channelId}/variable", CHANNEL);
		pathsToCommandTypes.put("/channels/{channelId}/snoop", SNOOPING_CREATION);
		pathsToCommandTypes.put(
				"/channels/{channelId}/snoop/{snoopId}",
				SNOOPING_CREATION
		);
		pathsToCommandTypes.put("/channels/{channelId}/dial", CHANNEL);
		pathsToCommandTypes.put("/channels/{channelId}/rtp_statistics", CHANNEL);
		pathsToCommandTypes.put("/channels/externalMedia", CHANNEL);

		pathsToCommandTypes.put("/bridges", BRIDGE_CREATION);
		pathsToCommandTypes.put("/bridges/{bridgeId}", BRIDGE_CREATION);
		pathsToCommandTypes.put("/bridges/{bridgeId}/addChannel", BRIDGE);
		pathsToCommandTypes.put("/bridges/{bridgeId}/removeChannel", BRIDGE);
		pathsToCommandTypes.put(
				"/bridges/{bridgeId}/videoSource/{channelId}",
				BRIDGE
		);
		pathsToCommandTypes.put("/bridges/{bridgeId}/videoSource", BRIDGE);
		pathsToCommandTypes.put("/bridges/{bridgeId}/moh", BRIDGE);
		pathsToCommandTypes.put("/bridges/{bridgeId}/play", PLAYBACK_CREATION);
		pathsToCommandTypes.put(
				"/bridges/{bridgeId}/play/{playbackId}",
				PLAYBACK_CREATION
		);
		pathsToCommandTypes.put("/bridges/{bridgeId}/record", RECORDING_CREATION);

		pathsToCommandTypes.put("/playbacks/{playbackId}", PLAYBACK);
		pathsToCommandTypes.put("/playbacks/{playbackId}/control", PLAYBACK);

		pathsToCommandTypes.put("/recordings/stored", RECORDING);
		pathsToCommandTypes.put("/recordings/stored/{recordingName}", RECORDING);
		pathsToCommandTypes.put(
				"/recordings/stored/{recordingName}/file",
				RECORDING
		);
		pathsToCommandTypes.put(
				"/recordings/stored/{recordingName}/copy",
				RECORDING
		);
		pathsToCommandTypes.put("/recordings/live/{recordingName}", RECORDING);
		pathsToCommandTypes.put("/recordings/live/{recordingName}/stop", RECORDING);
		pathsToCommandTypes.put(
				"/recordings/live/{recordingName}/pause",
				RECORDING
		);
		pathsToCommandTypes.put("/recordings/live/{recordingName}/mute", RECORDING);
	}

	private static final ObjectReader reader = new ObjectMapper().reader();

	private final boolean isResourceCreationCommand;
	private final Function<String, Option<Try<String>>> resourceIdUriExtractor;
	private final Function<String, Option<Try<String>>> resourceIdBodyExtractor;

	AriCommandType(
			final boolean isResourceCreationCommand,
			final Function<String, Option<Try<String>>> resourceIdUriExtractor,
			final Function<String, Option<Try<String>>> resourceIdBodyExtractor
	) {
		this.isResourceCreationCommand = isResourceCreationCommand;
		this.resourceIdUriExtractor = resourceIdUriExtractor;
		this.resourceIdBodyExtractor = resourceIdBodyExtractor;
	}

	public Option<String> extractResourceIdFromUri(final String uri) {
		final Optional<AriCommandType> ariCommandType = pathsToCommandTypes
				.entrySet()
				.stream()
				.filter(
						entry -> {
							final String path = entry.getKey();
							final String theHolyRegex = path.replaceAll("\\{[^}]+}", "[^\\\\/]+");
							return uri.matches(theHolyRegex);
						}
				)
				.findFirst()
				.map(Map.Entry::getValue);

		if (ariCommandType.isPresent()) {
			return Option
					.ofOptional(ariCommandType)
					.flatMap(
							type ->
									type.resourceIdUriExtractor.apply(uri).flatMap(Value::toOption)
					);
		} else {
			return Option.none();
		}
	}

	public Option<Try<String>> extractResourceIdFromBody(final String body) {
		return resourceIdBodyExtractor.apply(body);
	}

	public static AriCommandType fromRequestUri(String candidateUri) {
		final Optional<AriCommandType> ariCommandType = pathsToCommandTypes
				.entrySet()
				.stream()
				.filter(
						entry -> {
							final String path = entry.getKey();
							final String theHolyRegex = path.replaceAll("\\{[^}]+}", "[^\\\\/]+");
							return candidateUri.matches(theHolyRegex);
						}
				)
				.findFirst()
				.map(Map.Entry::getValue);

		return ariCommandType.orElse(UNKNOWN);
	}

	private static Function<String, Option<Try<String>>> resourceIdFromUri(
			final int resourceIdPosition
	) {
		return uri -> {
			if ("/channels/create".equals(uri)) {
				return Some(Try.failure(new Throwable("No ID present in URI")));
			}
			return Some(
					Try.of(() -> List.of(uri.split("/")).get(resourceIdPosition))
			);
		};
	}

  public boolean isResourceCreationCommand() {
    return isResourceCreationCommand;
  }

	private static Function<String, Option<Try<String>>> resourceIdFromBody(
			final String resourceIdXPath
	) {
		return body ->
				Some(
						Try
								.of(() -> reader.readTree(body))
								.flatMap(root -> Option.of(root).toTry())
								.toOption()
								.flatMap(root -> Option.of(root.at(resourceIdXPath)))
								.map(JsonNode::asText)
								.flatMap(type -> StringUtils.isBlank(type) ? None() : Some(type))
								.toTry(
										() ->
												new Throwable(
														String.format(
																"Failed to extract resourceId at path=%s from body=%s",
																resourceIdXPath,
																body
														)
												)
								)
				);
	}

	private static Option<Try<String>> notAvailable(final String bodyOrUri) { // TODO: 🤔
		return Some(Try.failure(new ExtractorNotAvailable(bodyOrUri)));
	}
}

class Companion {
	static final int RESOURCE_ID_POSITION = 2;
	static final int RESOURCE_ID_POSITION_ON_ANOTHER_RESOURCE = 4;
}
