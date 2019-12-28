package org.bremersee.groupman;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.reactive.server.WebTestClient;

/**
 * The application tests.
 */
@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles({"basic-auth"})
public class ApplicationTests {

  /**
   * The Context.
   */
  @Autowired
  ApplicationContext context;

  /**
   * The Web test client.
   */
  @Autowired
  WebTestClient webTestClient;

  /**
   * Setup tests.
   */
  @Before
  public void setup() {
    // https://docs.spring.io/spring-security/site/docs/current/reference/html/test-webflux.html
    // webTestClient.mutateWith(null)...
    WebTestClient
        .bindToApplicationContext(this.context)
        // add Spring Security test Support
        //.apply(springSecurity())
        .configureClient()
        //.filter(basicAuthentication())
        .build();
  }

  /**
   * Context loads.
   */
  @Test
  public void contextLoads() {
    webTestClient
        .get()
        .uri("/api/groups")
        .accept(MediaType.APPLICATION_JSON)
        .exchange()
        .expectStatus().isUnauthorized();
  }

}
