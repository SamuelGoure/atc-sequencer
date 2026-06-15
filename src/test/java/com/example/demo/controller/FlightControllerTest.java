package com.example.demo.controller;

import com.example.demo.model.Flight;
import com.example.demo.model.FlightCategory;
import com.example.demo.model.FlightStatus;
import com.example.demo.service.FlightService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(FlightController.class)
class FlightControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private ObjectMapper objectMapper;

    @MockitoBean
    private FlightService flightService;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
    }

    @Test
    void should_create_flight_and_return_201() throws Exception {
        Flight flight = new Flight("AF123", "Air France 123", "CDG", "ABJ",
                LocalDateTime.now().plusHours(3), FlightStatus.SCHEDULED,
                FlightCategory.COMMERCIAL, null, 0, "A");

        when(flightService.create(any(Flight.class))).thenReturn(flight);

        mockMvc.perform(post("/api/flights")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(flight)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.flightId").value("AF123"))
                .andExpect(jsonPath("$.callSign").value("Air France 123"));
    }

    @Test
    void should_return_404_for_unknown_flight() throws Exception {
        when(flightService.findById("UNKNOWN")).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/flights/UNKNOWN"))
                .andExpect(status().isNotFound());
    }

    @Test
    void should_return_400_for_invalid_status() throws Exception {
        mockMvc.perform(put("/api/flights/AF123/status")
                        .param("status", "INVALIDE"))
                .andExpect(status().isBadRequest());
    }
}
