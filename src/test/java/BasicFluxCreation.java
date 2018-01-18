import java.time.Duration;
import java.time.Instant;

import org.junit.Test;

import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public class BasicFluxCreation {

  @Test
  public void fluxAreLazy() {
    /*  ---
        This example demonstrates that flux are lazy.
        Being lazy means that nothing is executed until we call a specific method, subscribe for Flux and Mono.
     */
    Instant beginning = Instant.now();

    Flux<Integer> flux = Flux.range(1, 5).
      flatMap(i -> {
        try { Thread.sleep(100); return Mono.just(i * i); }
        catch (InterruptedException e) { return Mono.error(e); }
      });

    // Here, if the flux wasn't lazy, it would have taken at least 500ms to execute the above code.
    // Let's see what the result is :
    Instant step1 = Instant.now();
    System.out.println("After step1, program runs for : " + Utils.giveTimeDifference(beginning, step1));

    // Now let's subscribe to the Flux. We will log the value in the console.
    flux.subscribe(System.out::println);

    // Duration from the beginning :
    System.out.println("The whole test last : " + Utils.timePastFrom(beginning));
  }

  @Test
  public void fluxAreImmutable() {
     /*  ---
        This example demonstrates that flux are immutable.
        Being immutable means that when calling a method on the flux to modify it, it will not modify the flux itself,
        but return a new one.
     */

    // If we do that :
    Flux<Integer> flux = Flux.range(1, 5);
    flux.flatMap(i -> {
        try { Thread.sleep(100); return Mono.just(i * i); }
        catch (InterruptedException e) { return Mono.error(e); }
      });
    flux.subscribe(System.out::println);
    // It will display : 1 2 3 4 5, meaning that it is not the flux we modified.

    // In order to execute the flux we modified, we should write :
    Flux<Integer> flux2 = Flux.range(1, 5);
    Flux<Integer> flux3 = flux2.flatMap(i -> {
      System.out.println("Start");
      try { Thread.sleep(100); return Mono.just(i * i); }
      catch (InterruptedException e) { return Mono.error(e); }
    });
    flux3.subscribe(System.out::println);
    // Log : 1 4 9 16 25
    // Success !
  }

  @Test
  public void fluxCanBeInfinite() {
    /*  ---
        This example demonstrates that flux can be infinite.
        To do that, we just need to make the flux ticking, as if it was a clock.
     */
    Flux.interval(Duration.ofMillis(100)).
      map(i -> "Tick : " + i).
      subscribe(System.out::println);

    try { Thread.sleep(3000); }
    catch (InterruptedException e) { e.printStackTrace(); }
  }

  @Test
  public void infiniteFluxCanBeStopped() {
    /*  ---
        This example demonstrates that infinite flux can be stopped.
     */
    Disposable disposable = Flux.interval(Duration.ofMillis(100)).
      map(i -> "Tick : " + i).
      subscribe(System.out::println);

    try { Thread.sleep(1000); }
    catch (InterruptedException e) { e.printStackTrace(); }

    disposable.dispose();
    System.out.println("Stopped flux");

    try { Thread.sleep(2000); }
    catch (InterruptedException e) { e.printStackTrace(); }
    System.out.println("End of test");
  }

  @Test
  public void youCanSelectAsManyItemsInTheFluxAsYouWant() {
    /*  ---
        This example demonstrates that in a flux, we can select the first N elements.

        Here we will take 15 elements in the flux.
     */
    Flux.interval(Duration.ofMillis(100)).
      take(15).
      map(i -> "Tick : " + i).
      subscribe(System.out::println);

    try { Thread.sleep(3000); }
    catch (InterruptedException e) { e.printStackTrace(); }
    System.out.println("End of test.");
  }

  @Test
  public void fluxRunSequentially() {
    Flux.range(1, 3).
      flatMap(n -> {
        System.out.println("In flatMap n=" + n + " --- Thread is : " + Thread.currentThread().getName());
        try {
          Thread.sleep(100);
          System.out.println("After Thread.sleep n=" + n);
          return Mono.just(n);
        } catch (InterruptedException e) { return Mono.error(e); }
      }).
      map(n -> { System.out.println("In map n=" + n); return n; }).
      subscribe(System.out::println);
  }

}
