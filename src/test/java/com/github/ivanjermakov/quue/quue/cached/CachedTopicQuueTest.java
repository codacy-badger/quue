package com.github.ivanjermakov.quue.quue.cached;

import com.github.ivanjermakov.quue.element.CachedElement;
import org.junit.Test;
import reactor.test.StepVerifier;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

public class CachedTopicQuueTest {
	private static final String TOPIC = "topic";

	@Test
	public void shouldCompleteEmpty() {
		CachedTopicQuue<String, Integer> queue = new CachedTopicQuue<>();

		StepVerifier verifier = StepVerifier
				.create(queue.subscribe(TOPIC))
				.expectComplete()
				.verifyLater();

		queue.complete();

		verifier.verify();
	}

	@Test
	public void shouldWriteAndComplete() {
		CachedTopicQuue<String, Integer> queue = new CachedTopicQuue<>();

		StepVerifier verifier = StepVerifier
				.create(queue.subscribe(TOPIC))
				.assertNext(e -> assertThat(e.data()).isEqualTo(1))
				.assertNext(e -> assertThat(e.data()).isEqualTo(2))
				.expectComplete()
				.verifyLater();

		queue.send(TOPIC, 1);
		queue.send(TOPIC, 2);
		queue.complete(TOPIC);

		verifier.verify();
	}

	@Test
	public void shouldReadFromMultipleSubscribers() {
		CachedTopicQuue<String, Integer> queue = new CachedTopicQuue<>();

		List<StepVerifier> verifiers = IntStream
				.range(0, 10)
				.boxed()
				.map(i -> StepVerifier
						.create(queue.subscribe(TOPIC))
						.assertNext(e -> assertThat(e.data()).isEqualTo(1))
						.assertNext(e -> assertThat(e.data()).isEqualTo(2))
						.expectComplete()
						.verifyLater()
				)
				.collect(Collectors.toList());

		queue.send(TOPIC, 1);
		queue.send(TOPIC, 2);
		queue.complete(TOPIC);

		verifiers.forEach(StepVerifier::verify);
	}

	@Test
	public void shouldNotReadWroteAfterRead() {
		CachedTopicQuue<String, Integer> queue = new CachedTopicQuue<>();
		queue.send(TOPIC, 1);

		StepVerifier verifier = StepVerifier
				.create(queue.subscribe(TOPIC))
				.assertNext(e -> assertThat(e.data()).isEqualTo(1))
				.assertNext(e -> assertThat(e.data()).isEqualTo(2))
				.expectComplete()
				.verifyLater();

		queue.send(TOPIC, 2);
		queue.complete();

		verifier.verify();
	}

	@Test
	public void shouldSetCorrectIndices() {
		CachedTopicQuue<String, Integer> queue = new CachedTopicQuue<>();
		queue.send(TOPIC, 1);
		queue.send(TOPIC, 2);
		queue.complete();

		StepVerifier
				.create(queue.subscribe(TOPIC))
				.assertNext(e -> assertThat(e.index()).isEqualTo(0))
				.assertNext(e -> assertThat(e.index()).isEqualTo(1))
				.expectComplete()
				.verify();
	}

	@Test
	public void shouldSetCorrectTimestamps() {
		CachedTopicQuue<String, Integer> queue = new CachedTopicQuue<>();

		LocalDateTime timestamp1 = LocalDateTime.now();
		queue.send(TOPIC, 1);

		LocalDateTime timestamp2 = LocalDateTime.now();
		queue.send(TOPIC, 2);

		queue.complete();

		StepVerifier
				.create(queue.subscribe(TOPIC))
				.assertNext(e -> assertThat(ChronoUnit.SECONDS.between(timestamp1, e.timestamp())).isEqualTo(0))
				.assertNext(e -> assertThat(ChronoUnit.SECONDS.between(timestamp2, e.timestamp())).isEqualTo(0))
				.expectComplete()
				.verify();

		List<CachedElement<Integer>> elements = queue
				.subscribe(TOPIC)
				.collectList()
				.block();
		assertThat(elements).hasSize(2);
	}

	@Test
	public void shouldSubscribeWithOffset() {
		CachedTopicQuue<String, Integer> queue = new CachedTopicQuue<>();
		queue.send(TOPIC, 1);
		queue.send(TOPIC, 2);
		queue.complete();

		StepVerifier
				.create(queue.subscribe(TOPIC, 1))
				.assertNext(e -> assertThat(e.data()).isEqualTo(2))
				.expectComplete()
				.verify();
	}

	@Test
	public void shouldSubscribeWithAfter() {
		CachedTopicQuue<String, Integer> queue = new CachedTopicQuue<>();
		queue.send(TOPIC, 1);
		queue.send(TOPIC, 2);

		StepVerifier verifier = StepVerifier
				.create(queue.subscribe(TOPIC, LocalDateTime.now()))
				.assertNext(e -> assertThat(e.data()).isEqualTo(3))
				.assertNext(e -> assertThat(e.data()).isEqualTo(4))
				.expectComplete()
				.verifyLater();

		queue.send(TOPIC, 3);
		queue.send(TOPIC, 4);
		queue.complete();

		verifier.verify();
	}
}
