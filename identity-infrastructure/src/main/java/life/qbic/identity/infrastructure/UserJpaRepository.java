package life.qbic.identity.infrastructure;

import java.util.List;
import java.util.Optional;
import life.qbic.identity.domain.model.EmailAddress;
import life.qbic.identity.domain.model.User;
import life.qbic.identity.domain.model.UserId;
import life.qbic.identity.domain.repository.UserDataStorage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;


/**
 * <b>User JPA repository</b>
 *
 * <p>Implementation for the {@link UserDataStorage} interface.
 *
 * <p>This class serves as an adapter and proxies requests to an JPA implementation to interact
 * with persistent {@link User} data in the storage layer.
 *
 * <p>The actual JPA implementation is done by {@link QbicUserRepo}, which is injected as
 * dependency upon creation.
 *
 * @since 1.0.0
 */
@Component
public class UserJpaRepository implements UserDataStorage {

  private final QbicUserRepo userRepo;

  @Autowired
  public UserJpaRepository(QbicUserRepo userRepo) {
    this.userRepo = userRepo;
  }

  @Override
  public List<User> findUsersByEmailAddress(EmailAddress emailAddress) {
    return userRepo.findUsersByEmailAddress(emailAddress);
  }

  @Override
  public void save(User user) {
    userRepo.save(user);
  }

  @Override
  public Optional<User> findUserById(UserId id) {
    return Optional.ofNullable(userRepo.findUserById(id));
  }

  @Override
  public List<User> findAllActiveUsers() {
    return userRepo.findUsersByActiveTrue();
  }

  @Override
  public Optional<User> findUserByUserName(String userName) {
    return Optional.ofNullable(userRepo.findUserByUserName(userName));
  }

  @Override
  public List<User> queryActiveUsersWithFilter(String filter, Pageable pageable) {
    Specification<User> userSpecification = generateUserFilterSpecification(filter);
    return userRepo.findAll(userSpecification, pageable).getContent();
  }

  @Override
  public Optional<User> findByOidcIdEqualsAndOidcIssuerEquals(String oidcId, String oidcIssuer) {
    return userRepo.findByOidcIdEqualsAndOidcIssuerEquals(oidcId, oidcIssuer);
  }

  private Specification<User> generateUserFilterSpecification(String filter) {
    Specification<User> isBlankSpec = UserSpec.isBlank(filter);
    Specification<User> isFullName = UserSpec.isFullName(filter);
    Specification<User> isUserNameSpec = UserSpec.isUserName(filter);
    Specification<User> isOidc = UserSpec.isOidc(filter);
    Specification<User> isOidcIssuer = UserSpec.isOidcIssuer(filter);
    Specification<User> isActiveSpec = UserSpec.isActive();
    Specification<User> filterSpecification =
        Specification.anyOf(isFullName,
            isUserNameSpec,
            isOidc,
            isOidcIssuer
        );
    return Specification.where(isBlankSpec)
        .and(filterSpecification)
        .and(isActiveSpec);
  }

  private static class UserSpec {

    //If no filter was provided return all Users
    public static Specification<User> isBlank(String filter) {
      return (root, query, builder) -> {
        if (filter != null && filter.isBlank()) {
          return builder.conjunction();
        }
        return null;
      };
    }

    public static Specification<User> isUserName(String filter) {
      return (root, query, builder) ->
          builder.like(root.get("userName"), "%" + filter + "%");
    }

    public static Specification<User> isFullName(String filter) {
      return (root, query, builder) ->
          builder.like(root.get("fullName"), "%" + filter + "%");
    }

    // Should be extended if additional oidc providers are included, for now we only work with orcid
    public static Specification<User> isOidc(String filter) {
      return (root, query, builder) ->
          builder.like(root.get("oidcId"), "%" + filter + "%");
    }

    public static Specification<User> isOidcIssuer(String filter) {
      return (root, query, builder) ->
          builder.like(root.get("oidcIssuer"), "%" + filter + "%");
    }

    public static Specification<User> isActive() {
      return (root, query, builder) ->
          builder.isTrue(root.get("active"));
    }
  }
}
