package kz.spt.api.bootstrap.datatable;


import kz.spt.api.model.User;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

public final class UsersComparator {
    @EqualsAndHashCode
    @AllArgsConstructor
    @Getter
    static class Key {
        String name;
        Direction dir;
    }

    static Map<UsersComparator.Key, Comparator<User>> map = new HashMap<>();

    static {
        map.put(new UsersComparator.Key("username", Direction.asc), Comparator.comparing(User::getUsername));
        map.put(new UsersComparator.Key("username", Direction.desc), Comparator.comparing(User::getUsername).reversed());

        map.put(new UsersComparator.Key("email", Direction.asc), Comparator.comparing(User::getEmail));
        map.put(new UsersComparator.Key("email", Direction.desc), Comparator.comparing(User::getEmail).reversed());

        map.put(new UsersComparator.Key("firstName", Direction.asc), Comparator.comparing(User::getFirstName));
        map.put(new UsersComparator.Key("firstName", Direction.desc), Comparator.comparing(User::getFirstName).reversed());

        map.put(new UsersComparator.Key("lastName", Direction.asc), Comparator.comparing(User::getLastName));
        map.put(new UsersComparator.Key("lastName", Direction.desc), Comparator.comparing(User::getLastName).reversed());

//        map.put(new UsersComparator.Key("role", Direction.asc), Comparator.comparing(User::getRoles));
//        map.put(new UsersComparator.Key("role", Direction.desc), Comparator.comparing(User::getRoles).reversed());

    }

    public static Comparator<User> getComparator(String name, Direction dir) {
        return map.get(new UsersComparator.Key(name, dir));
    }

    private UsersComparator() {
    }
}
