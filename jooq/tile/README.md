# Jooq Tile

This is a tile for code generation. Migration scripts should be placed in `db/migration/<unit_name>`
and table names should not be prefixed with `lib_`, as such tables are expected to be defined by 
libraries, and will thus be excluded from code generation.