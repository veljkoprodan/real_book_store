package com.zuehlke.securesoftwaredevelopment.repository;

import com.zuehlke.securesoftwaredevelopment.domain.Rating;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

@Repository
public class RatingRepository {

    private static final Logger LOG = LoggerFactory.getLogger(RatingRepository.class);


    private DataSource dataSource;

    public RatingRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public void createOrUpdate(Rating rating) {
        String query = "SELECT movieId, userId, rating FROM ratings WHERE movieId = " + rating.getMovieId() + " AND userID = " + rating.getUserId();
        String query2 = "update ratings SET rating = ? WHERE movieId = ? AND userId = ?";
        String query3 = "insert into ratings(movieId, userId, rating) values (?, ?, ?)";

        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery(query)
        ) {
            if (rs.next()) {
                // 2. SonarQube
                try(PreparedStatement preparedStatement = connection.prepareStatement(query2)){
                    preparedStatement.setInt(1, rating.getRating());
                    preparedStatement.setInt(2, rating.getMovieId());
                    preparedStatement.setInt(3, rating.getUserId());
                    preparedStatement.executeUpdate();
                }
            } else {
                try(PreparedStatement preparedStatement = connection.prepareStatement(query3)){
                    preparedStatement.setInt(1, rating.getMovieId());
                    preparedStatement.setInt(2, rating.getUserId());
                    preparedStatement.setInt(3, rating.getRating());
                    preparedStatement.executeUpdate();
                }
            }
            LOG.info("User " + rating.getUserId() + " rated movie " + rating.getMovieId() +
                    " a " + rating.getRating() + " out of 5");
        } catch (SQLException e) {
            LOG.warn("Failed to create or update rating for movie " + rating.getMovieId());
        }
    }

    public List<Rating> getAll(String movieId) {
        List<Rating> ratingList = new ArrayList<>();
        String query = "SELECT movieId, userId, rating FROM ratings WHERE movieId = " + movieId;
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery(query)) {
            while (rs.next()) {
                ratingList.add(new Rating(rs.getInt(1), rs.getInt(2), rs.getInt(3)));
            }
        } catch (SQLException e) {
            LOG.warn("Failed to retrieve rating list for movie " + movieId, e);
        }
        return ratingList;
    }
}
