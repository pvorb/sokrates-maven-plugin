/*
 * This file is generated by jOOQ.
*/
package de.vorb.sokrates.db.jooq.tables;


import de.vorb.sokrates.db.jooq.Indexes;
import de.vorb.sokrates.db.jooq.Keys;
import de.vorb.sokrates.db.jooq.Public;
import de.vorb.sokrates.db.jooq.tables.records.TagRecord;

import java.util.Arrays;
import java.util.List;

import javax.annotation.Generated;

import org.jooq.Field;
import org.jooq.Identity;
import org.jooq.Index;
import org.jooq.Name;
import org.jooq.Schema;
import org.jooq.Table;
import org.jooq.TableField;
import org.jooq.UniqueKey;
import org.jooq.impl.DSL;
import org.jooq.impl.TableImpl;


/**
 * This class is generated by jOOQ.
 */
@Generated(
    value = {
        "http://www.jooq.org",
        "jOOQ version:3.10.2"
    },
    comments = "This class is generated by jOOQ"
)
@SuppressWarnings({ "all", "unchecked", "rawtypes" })
public class Tag extends TableImpl<TagRecord> {

    private static final long serialVersionUID = 615398103;

    /**
     * The reference instance of <code>PUBLIC.TAG</code>
     */
    public static final Tag TAG = new Tag();

    /**
     * The class holding records for this type
     */
    @Override
    public Class<TagRecord> getRecordType() {
        return TagRecord.class;
    }

    /**
     * The column <code>PUBLIC.TAG.ID</code>.
     */
    public final TableField<TagRecord, Long> ID = createField("ID", org.jooq.impl.SQLDataType.BIGINT.nullable(false).identity(true), this, "");

    /**
     * The column <code>PUBLIC.TAG.NAME</code>.
     */
    public final TableField<TagRecord, String> NAME = createField("NAME", org.jooq.impl.SQLDataType.VARCHAR(2147483647).nullable(false), this, "");

    /**
     * Create a <code>PUBLIC.TAG</code> table reference
     */
    public Tag() {
        this(DSL.name("TAG"), null);
    }

    /**
     * Create an aliased <code>PUBLIC.TAG</code> table reference
     */
    public Tag(String alias) {
        this(DSL.name(alias), TAG);
    }

    /**
     * Create an aliased <code>PUBLIC.TAG</code> table reference
     */
    public Tag(Name alias) {
        this(alias, TAG);
    }

    private Tag(Name alias, Table<TagRecord> aliased) {
        this(alias, aliased, null);
    }

    private Tag(Name alias, Table<TagRecord> aliased, Field<?>[] parameters) {
        super(alias, null, aliased, parameters, "");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Schema getSchema() {
        return Public.PUBLIC;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Index> getIndexes() {
        return Arrays.<Index>asList(Indexes.CONSTRAINT_INDEX_1, Indexes.IDX_TAG_NAME, Indexes.PRIMARY_KEY_1);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Identity<TagRecord, Long> getIdentity() {
        return Keys.IDENTITY_TAG;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public UniqueKey<TagRecord> getPrimaryKey() {
        return Keys.CONSTRAINT_1;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<UniqueKey<TagRecord>> getKeys() {
        return Arrays.<UniqueKey<TagRecord>>asList(Keys.CONSTRAINT_1, Keys.CONSTRAINT_14);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Tag as(String alias) {
        return new Tag(DSL.name(alias), this);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Tag as(Name alias) {
        return new Tag(alias, this);
    }

    /**
     * Rename this table
     */
    @Override
    public Tag rename(String name) {
        return new Tag(DSL.name(name), null);
    }

    /**
     * Rename this table
     */
    @Override
    public Tag rename(Name name) {
        return new Tag(name, null);
    }
}