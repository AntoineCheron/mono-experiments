import java.util.Arrays;
import java.util.List;
import java.util.Random;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.ParallelFlux;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

public class RandomPersonService {

  Random random = new Random();
  RemotePersonRepository personRepository = new RemotePersonRepository();

  Mono<Json> getRandomPersonInOneSecond() {
    return Mono.fromSupplier(() -> random.nextInt(100)).
      map((n) -> n % 5).
      map(personRepository::getPerson). // last 1 second
      map(Person::toJson);
  }
}

class RemotePersonRepository {
  final Person FOO = new Person("foo", "bar", (short) 1);
  final Person TOTO = new Person("toto", "titi", (short) 12);
  final Person BOB = new Person("sponge", "bob", (short) 100);
  final Person JCB = new Person("jcb", "b", (short) 134);
  final Person GASTON = new Person("gaston", "lagaffe", (short) 1);
  final List<Person> persons = Arrays.asList(FOO, TOTO, BOB, JCB, GASTON);

  Person getPerson(int i) {
    TestUtils.sleep(1000); // Makes the request last 1 second
    return persons.get(i);
  }
}

class Person {
  final String firstName;
  final String familyName;
  final Short age;

  public Person(String firstName, String familyName, Short age) {
    this.firstName = firstName; this.familyName = familyName; this.age = age;
  }

  public Json toJson() {
    return new Json("{ " +
      "\"firstName\": \"" + firstName + "\"," +
      "\"familyName\": \"" + familyName + "\"," +
      "\"age\": " + age +
    "}");
  }
}

class Json {
  final String value;
  Json(String value) { this.value = value; }
  public String get() { return value; }
}

class Main {
  static final int NUMBER_OF_CALLS = 100; // 10, 100, 1.000, 100.000
  static final Scheduler scheduler = Schedulers.elastic(); // Immediate | Single | Parallel | Elastic

  public static void main(String[] args) {
//    long duration = TestUtils.mesureTaskExecution(
//      () -> {
//        Counter counter = new Counter();
//        makeNRequestOnScheduler3(NUMBER_OF_CALLS, scheduler).
//          map(Json::get).
//          subscribe((stringified) -> {
//            System.out.println(stringified);
//            counter.next();
//          });
//
//        while (counter.get() != NUMBER_OF_CALLS) {
//          TestUtils.sleep(20);
//        }
//      }
//    );

    long duration = TestUtils.mesureTaskExecution(makeNRequestOnScheduler4(NUMBER_OF_CALLS, scheduler));
    System.out.println("\n\n\n" + duration);
  }

  static Flux<Json> makeNRequestOnScheduler1(int n, Scheduler scheduler) {
    final RandomPersonService service = new RandomPersonService();
    return Flux.range(1, n).
      flatMap((i) -> service.getRandomPersonInOneSecond()).
      subscribeOn(scheduler);
  }

  static ParallelFlux<Json> makeNRequestOnScheduler3(int n, Scheduler scheduler) {
    final RandomPersonService service = new RandomPersonService();
    return Flux.range(1, n).
      parallel().runOn(scheduler).
      flatMap((i) -> service.getRandomPersonInOneSecond());
  }

  static Runnable makeNRequestOnScheduler4(int n, Scheduler scheduler) {
    final RandomPersonService service = new RandomPersonService();
    Counter counter = new Counter();
    return () -> {
      Flux.range(1, n).
        map((i) -> service.getRandomPersonInOneSecond()).
        toStream().
        forEach(
          (mono) -> mono.subscribeOn(scheduler).subscribe((json) -> counter.next())
        );
      while (counter.get() != n) {
        TestUtils.sleep(20);
      }
    };
  }

  static ParallelFlux<Json> makeNRequestOnScheduler2(int n, Scheduler scheduler) {
    final RandomPersonService service = new RandomPersonService();
    return Flux.range(1, n).
      parallel().
      flatMap((i) -> service.getRandomPersonInOneSecond()).
      runOn(scheduler);
  }

  private static class Counter {
    private long count = 0;
    synchronized void next() { this.count ++; }
    long get() { return this.count; }
  }
}
