package com.extractor.unraveldocs.elasticsearch.document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.*;

import java.time.OffsetDateTime;

/**
 * Elasticsearch document for indexing user data.
 * Used for admin user search functionality.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(indexName = "users")
public class UserSearchIndex {

    @Id
    private String id;

    /**
     * User's first name (searchable).
     */
    @MultiField(mainField = @Field(type = FieldType.Text, analyzer = "standard"), otherFields = {
            @InnerField(suffix = "keyword", type = FieldType.Keyword)
    })
    private String firstName;

    /**
     * User's last name (searchable).
     */
    @MultiField(mainField = @Field(type = FieldType.Text, analyzer = "standard"), otherFields = {
            @InnerField(suffix = "keyword", type = FieldType.Keyword)
    })
    private String lastName;

    /**
     * User's email address.
     */
    @Field(type = FieldType.Keyword)
    private String email;

    /**
     * User's role (USER, ADMIN, etc.).
     */
    @Field(type = FieldType.Keyword)
    private String role;

    /**
     * Whether the user account is active.
     */
    @Field(type = FieldType.Boolean)
    private Boolean isActive;

    /**
     * Whether the user's email is verified.
     */
    @Field(type = FieldType.Boolean)
    private Boolean isVerified;

    /**
     * Whether the user is a platform admin.
     */
    @Field(type = FieldType.Boolean)
    private Boolean isPlatformAdmin;

    /**
     * Whether the user is an team admin.
     */
    @Field(type = FieldType.Boolean)
    private Boolean isOrganizationAdmin;

    /**
     * User's country.
     */
    @Field(type = FieldType.Keyword)
    private String country;

    /**
     * User's profession.
     */
    @MultiField(mainField = @Field(type = FieldType.Text, analyzer = "standard"), otherFields = {
            @InnerField(suffix = "keyword", type = FieldType.Keyword)
    })
    private String profession;

    /**
     * User's team (searchable).
     */
    @MultiField(mainField = @Field(type = FieldType.Text, analyzer = "standard"), otherFields = {
            @InnerField(suffix = "keyword", type = FieldType.Keyword)
    })
    private String organization;

    /**
     * Profile picture URL.
     */
    @Field(type = FieldType.Keyword, index = false)
    private String profilePicture;

    /**
     * Current subscription plan name.
     */
    @Field(type = FieldType.Keyword)
    private String subscriptionPlan;

    /**
     * Subscription status.
     */
    @Field(type = FieldType.Keyword)
    private String subscriptionStatus;

    /**
     * Last login timestamp.
     */
    @Field(type = FieldType.Date, format = DateFormat.date_optional_time)
    private OffsetDateTime lastLogin;

    /**
     * Account creation timestamp.
     */
    @Field(type = FieldType.Date, format = DateFormat.date_optional_time)
    private OffsetDateTime createdAt;

    /**
     * Last update timestamp.
     */
    @Field(type = FieldType.Date, format = DateFormat.date_optional_time)
    private OffsetDateTime updatedAt;

    /**
     * Total number of documents uploaded by user.
     */
    @Field(type = FieldType.Integer)
    private Integer documentCount;
}
