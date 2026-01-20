package io.github.kankan.demo.di

import io.github.kankan.demo.feature.board.BoardViewModel
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

val appModule = module {
    // Using singleOf instead of viewModelOf for iOS compatibility
    singleOf(::BoardViewModel)
}
