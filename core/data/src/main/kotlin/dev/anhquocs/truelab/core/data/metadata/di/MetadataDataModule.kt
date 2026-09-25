package dev.anhquocs.truelab.core.data.metadata.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dev.anhquocs.truelab.core.data.metadata.repository.DatasetMetadataRepositoryImpl
import dev.anhquocs.truelab.core.domain.metadata.repository.DatasetMetadataRepository
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class MetadataDataModule {

    @Binds
    @Singleton
    abstract fun bindDatasetMetadataRepository(
        datasetMetadataRepositoryImpl: DatasetMetadataRepositoryImpl
    ): DatasetMetadataRepository
}
