package com.example.demo.repository;

import com.example.demo.model.Flight;
import com.example.demo.model.FlightCategory;
import com.example.demo.model.FlightStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FlightRepository extends JpaRepository<Flight, String> {

    List<Flight> findByStatus(FlightStatus status);

    List<Flight> findByCategory(FlightCategory category);

    List<Flight> findByDelayMinutesGreaterThan(int minutes);

    List<Flight> findByRunway(String runway);

    List<Flight> findByStatusOrderBySequenceNumberAsc(FlightStatus status);
}
